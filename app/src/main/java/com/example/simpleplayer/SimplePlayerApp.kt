package com.example.simpleplayer

import android.app.Application
import com.example.simpleplayer.data.AppDatabase

class SimplePlayerApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        // Il database viene creato alla prima chiamata a `database`
    }
}
