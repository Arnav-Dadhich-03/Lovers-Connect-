// java/com/arnav/loversconnect/Message.kt
package com.arnav.loversconnect
import java.util.HashMap

data class Message(
    var id: String? = null,
    val text: String? = null, // Text is now optional
    val imageUrl: String? = null, // URL for the image
    val senderId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "TEXT", // Can be "TEXT" or "IMAGE"
    val deletedFor: HashMap<String, Boolean> = HashMap()
)