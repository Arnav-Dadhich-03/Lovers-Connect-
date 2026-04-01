// java/com/arnav/loversconnect/ImageViewerActivity.kt
package com.arnav.loversconnect

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView

class ImageViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        val photoView = findViewById<PhotoView>(R.id.photo_view)
        val imageUrl = intent.getStringExtra("IMAGE_URL")

        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .into(photoView)
        }
    }
}