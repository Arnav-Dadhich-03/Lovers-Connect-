// java/com/arnav/loversconnect/DisplayableItem.kt
package com.arnav.loversconnect

sealed class DisplayableItem {
    data class MessageItem(val message: Message) : DisplayableItem()
    data class ImageGroup(val messages: List<Message>) : DisplayableItem()
}