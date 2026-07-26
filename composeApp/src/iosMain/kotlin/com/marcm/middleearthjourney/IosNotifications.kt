package com.marcm.middleearthjourney

import platform.Foundation.NSUUID
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/** Pide permiso de notificaciones (llamar una vez al arrancar). */
fun requestNotificationAuthorization() {
    val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
    UNUserNotificationCenter.currentNotificationCenter()
        .requestAuthorizationWithOptions(options) { _, _ -> }
}

/** Lanza una notificación local con el suceso del camino. */
fun postEventNotification(title: String, body: String) {
    val content = UNMutableNotificationContent()
    content.setTitle(title)
    content.setBody(body)
    content.setSound(UNNotificationSound.defaultSound)
    // Pequeño retardo para que la entregue aunque la app esté en primer plano.
    val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, repeats = false)
    val request = UNNotificationRequest.requestWithIdentifier(
        identifier = "meej_event_" + NSUUID().UUIDString,
        content = content,
        trigger = trigger,
    )
    UNUserNotificationCenter.currentNotificationCenter()
        .addNotificationRequest(request) { _ -> }
}
