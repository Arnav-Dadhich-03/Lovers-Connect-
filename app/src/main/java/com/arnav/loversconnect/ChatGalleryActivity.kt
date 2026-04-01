// java/com/arnav/loversconnect/ChatGalleryActivity.kt
package com.arnav.loversconnect

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ChatGalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_gallery)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val galleryRecyclerView = findViewById<RecyclerView>(R.id.galleryRecyclerView)
        val layoutManager = GridLayoutManager(this, 3)
        galleryRecyclerView.layoutManager = layoutManager

        val galleryItems = ArrayList<GalleryItem>()
        val adapter = GalleryAdapter(galleryItems)
        galleryRecyclerView.adapter = adapter

        // Make headers span the full width of the grid
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter.getItemViewType(position) == 0) 3 else 1 // 0 is TYPE_HEADER
            }
        }

        val dbRef = FirebaseDatabase.getInstance().getReference("chats")
        dbRef.orderByChild("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val imageMessages = snapshot.children
                    .mapNotNull { it.getValue(Message::class.java) }
                    .filter { it.type == "IMAGE" }
                    .sortedByDescending { it.timestamp } // Newest first

                val groupedMap = imageMessages.groupBy {
                    // Group by date, e.g., "July 22, 2025"
                    SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(it.timestamp))
                }

                galleryItems.clear()
                for ((date, messagesOnDate) in groupedMap) {
                    galleryItems.add(GalleryItem.Header(date)) // Add the header
                    messagesOnDate.forEach { message ->
                        galleryItems.add(GalleryItem.Image(message)) // Add the images for that day
                    }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}