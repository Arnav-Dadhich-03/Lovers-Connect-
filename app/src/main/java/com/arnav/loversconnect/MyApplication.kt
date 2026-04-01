// java/com/arnav/loversconnect/MyApplication.kt
package com.arnav.loversconnect

import android.app.Application
import com.cloudinary.android.MediaManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // --- CONFIGURE CLOUDINARY HERE ---
        val config = mutableMapOf<String, String>()
        config["cloud_name"] = "dzxr1nbff"
        config["api_key"] = "477773962919154"
        config["api_secret"] = "3li_yh667d6Kx1HVy7KGqXIdhEM"

        MediaManager.init(this, config)
    }
}