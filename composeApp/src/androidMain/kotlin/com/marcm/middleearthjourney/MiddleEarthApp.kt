package com.marcm.middleearthjourney

import android.app.Application
import com.marcm.middleearthjourney.data.StepRepository

class MiddleEarthApp : Application() {

    lateinit var stepRepository: StepRepository
        private set

    override fun onCreate() {
        super.onCreate()
        stepRepository = StepRepository(this)
    }
}
