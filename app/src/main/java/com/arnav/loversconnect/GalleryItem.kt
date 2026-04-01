// java/com/arnav/loversconnect/GalleryItem.kt
package com.arnav.loversconnect

sealed class GalleryItem {
    data class Image(val message: Message) : GalleryItem()
    data class Header(val date: String) : GalleryItem()
}