"""Publica una release de Middle Earth Journey y actualiza el manifiesto de updates.

Ritual completo (hermano del de DracPDF, Crónicas del Apetito y Grimorio de Salud):
build del APK de release FIRMADO, lectura del versionCode/versionName (fuente única:
composeApp/build.gradle.kts), cálculo del sha256, verificación de coherencia (el
versionCode del APK construido, leído con aapt2, debe coincidir con el que se
escribirá en el manifiesto, y ser mayor que el ya publicado; la firma debe seguir
siendo la misma de siempre — si algo no cuadra, aborta), creación de la Release en
GitHub con el asset (gh CLI, verificando antes gh auth status) y publicación del
manifiesto docs/updates.json en GitHub Pages (commit + push), verificando después
que la URL pública ya sirve el versionCode nuevo (reintentando por la caché del CDN).

Secretos: la firma sale de keystore.properties (fuera del repo, gitignored) o de
variables de entorno MEJ_STORE_FILE / MEJ_STORE_PASSWORD / MEJ_KEY_ALIAS /
MEJ_KEY_PASSWORD. Si faltan, aborta con mensaje claro. Ningún secreto se escribe
en el repo.

Uso:
    python scripts/publicar_release.py              # construye y publica
    python scripts/publicar_release.py --dry-run    # prepara sin publicar
    python scripts/publicar_release.py --notas "…"  # notas de la versión
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import time
import urllib.error
import urllib.request
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[1]
BUILD_GRADLE = RAIZ / "composeApp" / "build.gradle.kts"
MANIFIESTO = RAIZ / "docs" / "updates.json"
FIRMA_ESPERADA = RAIZ / "scripts" / "firma_esperada.txt"
APK_RELEASE = (
    RAIZ / "composeApp" / "build" / "outputs" / "apk" / "release" / "composeApp-release.apk"
)

_REPO = "marcmayol/middle_earth_journey"
_PAGES_URL = "https://marcmayol.com/middle_earth_journey/updates.json"
_RAMA = "main"  # rama que sirve GitHub Pages (/docs) y a la que se empuja el manifiesto
_NOMBRE_ASSET = "middle-earth-journey"
_TITULO = "Middle Earth Journey"
_CHECK_HORAS = 24
_ENV_FIRMA = (
    "MEJ_STORE_FILE",
    "MEJ_STORE_PASSWORD",
    "MEJ_KEY_ALIAS",
    "MEJ_KEY_PASSWORD",
)


# --- utilidades ---------------------------------------------------------------

def _ejecutar(cmd: list[str], **kw) -> None:
    print("»", " ".join(cmd))
    if subprocess.call(cmd, cwd=str(RAIZ), **kw) != 0:
        raise SystemExit(f"Falló: {' '.join(cmd)}")

def _salida(cmd: list[str]) -> str:
    return subprocess.run(
        cmd, cwd=str(RAIZ), capture_output=True, text=True
    ).stdout

def sha256(ruta: Path) -> str:
    h = hashlib.sha256()
    with ruta.open("rb") as f:
        for bloque in iter(lambda: f.read(65536), b""):
            h.update(bloque)
    return h.hexdigest()

def _gradlew() -> str:
    return "gradlew.bat" if os.name == "nt" else "./gradlew"


# --- versión (fuente única: composeApp/build.gradle.kts) ----------------------

def leer_version() -> tuple[int, str]:
    texto = BUILD_GRADLE.read_text(encoding="utf-8")
    vc = re.search(r"versionCode\s*=\s*(\d+)", texto)
    vn = re.search(r'versionName\s*=\s*"([^"]+)"', texto)
    if not vc or not vn:
        raise SystemExit(
            "No se pudo leer versionCode/versionName de composeApp/build.gradle.kts."
        )
    return int(vc.group(1)), vn.group(1)


# --- firma --------------------------------------------------------------------

def asegurar_firma() -> None:
    """Comprueba que hay credenciales de firma; si vienen por env, las materializa
    en un keystore.properties temporal (borrado al terminar). Nunca sobrescribe uno
    existente ni deja secretos en el repo."""
    props = RAIZ / "keystore.properties"
    if props.exists():
        print("Firma: usando keystore.properties existente.")
        return
    if all(os.environ.get(k) for k in _ENV_FIRMA):
        print("Firma: usando variables de entorno (keystore.properties temporal).")
        props.write_text(
            f"storeFile={os.environ['MEJ_STORE_FILE']}\n"
            f"storePassword={os.environ['MEJ_STORE_PASSWORD']}\n"
            f"keyAlias={os.environ['MEJ_KEY_ALIAS']}\n"
            f"keyPassword={os.environ['MEJ_KEY_PASSWORD']}\n",
            encoding="utf-8",
        )
        import atexit
        atexit.register(lambda: props.exists() and props.unlink())
        return
    raise SystemExit(
        "Faltan credenciales de firma. Copia keystore.properties.example a "
        "keystore.properties (no se versiona) y rellénalo, o define las variables "
        f"de entorno: {', '.join(_ENV_FIRMA)}."
    )


# --- herramientas del SDK (verificación de coherencia) ------------------------

def _sdk_dir() -> Path:
    local = RAIZ / "local.properties"
    if local.exists():
        m = re.search(r"sdk\.dir=(.+)", local.read_text(encoding="utf-8"))
        if m:
            return Path(m.group(1).strip().replace("\\\\", "\\").replace("\\:", ":"))
    for env in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(env):
            return Path(os.environ[env])
    raise SystemExit("No encuentro el Android SDK (local.properties o ANDROID_HOME).")

def _build_tool(nombre: str) -> Path:
    """Ruta a una herramienta de build-tools, la de versión más alta disponible."""
    exe = f"{nombre}.exe" if os.name == "nt" else nombre
    candidatos = sorted((_sdk_dir() / "build-tools").glob(f"*/{exe}"), reverse=True)
    if not candidatos:
        # apksigner es un .bat en Windows; aapt2 sí es .exe.
        alt = sorted((_sdk_dir() / "build-tools").glob(f"*/{nombre}.bat"), reverse=True)
        if alt:
            return alt[0]
        raise SystemExit(f"No encuentro {nombre} en build-tools del SDK.")
    return candidatos[0]

def version_code_del_apk(apk: Path) -> int:
    salida = _salida([str(_build_tool("aapt2")), "dump", "badging", str(apk)])
    m = re.search(r"versionCode='(\d+)'", salida)
    if not m:
        raise SystemExit("No pude leer el versionCode del APK con aapt2.")
    return int(m.group(1))

def huella_firma(apk: Path) -> str | None:
    """SHA-256 del certificado de firma del APK, o None si apksigner no está."""
    try:
        salida = _salida([str(_build_tool("apksigner")), "verify", "--print-certs", str(apk)])
    except SystemExit:
        return None
    m = re.search(r"certificate SHA-256 digest:\s*([0-9a-fA-F]+)", salida)
    return m.group(1).lower() if m else None


# --- manifiesto ---------------------------------------------------------------

def url_release(version_name: str) -> str:
    return (
        f"https://github.com/{_REPO}/releases/download/"
        f"v{version_name}/{_NOMBRE_ASSET}-v{version_name}.apk"
    )

def generar_manifiesto(vc: int, vn: str, sha: str, notas: str) -> dict:
    return {
        "versionCode": vc,
        "versionName": vn,
        "url": url_release(vn),
        "sha256": sha,
        "notas": notas or f"{_TITULO} {vn}.",
        "check_horas": _CHECK_HORAS,
    }

def version_code_publicado() -> int | None:
    """versionCode que sirve ahora mismo la URL pública (None si aún no hay ninguno).

    Se lee de la red y no de git a propósito: lo que importa es lo que las apps
    instaladas ven. El working tree no vale porque un --dry-run previo ya lo ha
    reescrito con la versión que preparamos, y el manifiesto commiteado tampoco,
    porque puede haberse commiteado antes de llegar a publicarse."""
    try:
        with urllib.request.urlopen(_PAGES_URL, timeout=15) as r:
            return int(json.loads(r.read().decode("utf-8"))["versionCode"])
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return None  # primera publicación: no hay manifiesto que superar
        raise SystemExit(f"No pude leer el manifiesto publicado ({e}). Aborto.")
    except Exception as e:  # noqa: BLE001
        raise SystemExit(
            f"No pude leer el manifiesto publicado en {_PAGES_URL} ({e.__class__.__name__}). "
            "Sin saber qué versión hay publicada no puedo garantizar que esta la supere. "
            "Reintenta cuando haya red. Aborto."
        )

def rama_actual() -> str:
    return _salida(["git", "rev-parse", "--abbrev-ref", "HEAD"]).strip()

def verificar_coherencia(vc_declarado: int, apk: Path, manifiesto: dict) -> None:
    """Cinturón: el versionCode del APK construido, el declarado y el del manifiesto
    coinciden; el sha256 del manifiesto es el del APK real; la versión sube respecto
    a la publicada; y la firma sigue siendo la misma (si cambia, ninguna instalación
    existente podrá actualizarse)."""
    vc_apk = version_code_del_apk(apk)
    if vc_apk != vc_declarado:
        raise SystemExit(
            f"El APK construido tiene versionCode {vc_apk}, pero build.gradle.kts "
            f"declara {vc_declarado}. Aborto."
        )
    if manifiesto["versionCode"] != vc_declarado:
        raise SystemExit("El versionCode del manifiesto no coincide con el declarado.")
    if manifiesto["sha256"] != sha256(apk):
        raise SystemExit("El sha256 del manifiesto no coincide con el APK construido.")

    publicado = version_code_publicado()
    if publicado is not None and vc_declarado <= publicado:
        raise SystemExit(
            f"El versionCode {vc_declarado} no supera al ya publicado ({publicado}): "
            "nadie detectaría la actualización. Sube el versionCode. Aborto."
        )

    huella = huella_firma(apk)
    if huella is None:
        print("Aviso: no pude leer la firma del APK (apksigner no disponible).")
        return
    if FIRMA_ESPERADA.is_file():
        esperada = FIRMA_ESPERADA.read_text(encoding="utf-8").strip().lower()
        if esperada and esperada != huella:
            raise SystemExit(
                "La firma del APK ha cambiado respecto a la de las versiones ya "
                f"distribuidas ({esperada[:16]}… → {huella[:16]}…). Con otra firma, "
                "ninguna instalación existente puede actualizarse. Aborto."
            )
        print(f"Firma verificada: {huella[:16]}…")
    else:
        FIRMA_ESPERADA.write_text(huella + "\n", encoding="utf-8")
        print(f"Firma registrada por primera vez en {FIRMA_ESPERADA.name}: {huella[:16]}…")


# --- construcción -------------------------------------------------------------

def construir() -> Path:
    asegurar_firma()
    _ejecutar([_gradlew(), ":composeApp:assembleRelease"])
    if not APK_RELEASE.is_file():
        raise SystemExit(f"No se generó el APK de release: {APK_RELEASE}")
    return APK_RELEASE

def preparar(notas: str) -> tuple[dict, Path]:
    """Construye, genera y escribe el manifiesto tras verificar coherencia."""
    vc, vn = leer_version()
    apk = construir()
    manifiesto = generar_manifiesto(vc, vn, sha256(apk), notas)
    verificar_coherencia(vc, apk, manifiesto)
    MANIFIESTO.parent.mkdir(parents=True, exist_ok=True)
    MANIFIESTO.write_text(
        json.dumps(manifiesto, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )
    return manifiesto, apk


# --- publicación --------------------------------------------------------------

def _asset_con_nombre(apk: Path, vn: str) -> Path:
    destino = apk.with_name(f"{_NOMBRE_ASSET}-v{vn}.apk")
    if destino != apk:
        destino.write_bytes(apk.read_bytes())
    return destino

def verificar_gh() -> None:
    if subprocess.call(["gh", "auth", "status"]) != 0:
        raise SystemExit("gh no está autenticado. Ejecuta: gh auth login")

def verificar_rama() -> None:
    """El manifiesto se sirve desde la rama de Pages: publicar desde otra dejaría la
    Release creada pero a nadie avisado."""
    actual = rama_actual()
    if actual != _RAMA:
        raise SystemExit(
            f"Estás en la rama '{actual}' y Pages sirve '{_RAMA}'. Cambia de rama "
            "antes de publicar. Aborto."
        )

def publicar(apk: Path, manifiesto: dict, notas: str) -> None:
    vn = manifiesto["versionName"]
    asset = _asset_con_nombre(apk, vn)
    _ejecutar([
        "gh", "release", "create", f"v{vn}", str(asset),
        "--repo", _REPO,
        "--title", f"{_TITULO} {vn}",
        "--notes", notas or f"{_TITULO} {vn}.",
    ])
    _ejecutar(["git", "add", str(MANIFIESTO), str(FIRMA_ESPERADA)])
    _ejecutar(["git", "commit", "-m", f"Publica el manifiesto de la v{vn}"])
    _ejecutar(["git", "push", "origin", _RAMA])

def verificar_url_publica(vc_esperado: int, intentos: int = 30, espera_s: int = 10) -> None:
    """La URL de Pages puede tardar por la caché del CDN: reintenta unos minutos."""
    for i in range(1, intentos + 1):
        try:
            with urllib.request.urlopen(_PAGES_URL, timeout=15) as r:
                data = json.loads(r.read().decode("utf-8"))
            if data.get("versionCode") == vc_esperado:
                print(f"URL pública OK: sirve versionCode {vc_esperado}.")
                return
            print(f"[{i}/{intentos}] Pages sirve {data.get('versionCode')}, esperaba {vc_esperado}…")
        except Exception as e:  # noqa: BLE001
            print(f"[{i}/{intentos}] Aún no disponible ({e.__class__.__name__})…")
        time.sleep(espera_s)
    raise SystemExit(
        "La URL pública no sirvió el versionCode nuevo a tiempo. La Release SÍ se "
        "creó; revisa GitHub Pages (rama/carpeta /docs) y la caché del CDN."
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=f"Publica una release de {_TITULO}.")
    parser.add_argument("--dry-run", action="store_true", help="prepara sin publicar")
    parser.add_argument("--notas", default="", help="notas de la versión")
    args = parser.parse_args(argv)

    if not args.dry_run:
        verificar_gh()
        verificar_rama()

    manifiesto, apk = preparar(args.notas)
    print(f"Manifiesto v{manifiesto['versionName']} "
          f"(versionCode {manifiesto['versionCode']}, sha256 {manifiesto['sha256'][:12]}…)")
    print(f"APK: {apk}")

    if args.dry_run:
        print("--dry-run: preparado sin publicar (Release y manifiesto no subidos).")
        return 0

    publicar(apk, manifiesto, args.notas)
    verificar_url_publica(manifiesto["versionCode"])
    print(f"Release v{manifiesto['versionName']} publicada y manifiesto en Pages.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
