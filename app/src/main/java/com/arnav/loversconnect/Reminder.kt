// java/com/arnav/loversconnect/Reminder.kt
package com.arnav.loversconnect

import java.util.Date

data class Reminder(
    var id: String? = null,
    val title: String = "",
    val timestamp: Long = Date().time,
    val createdByUid: String = ""
)