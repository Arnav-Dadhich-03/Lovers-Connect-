// java/com/arnav/loversconnect/ChatAdapter.kt
package com.arnav.loversconnect

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatAdapter(private val itemList: ArrayList<DisplayableItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Define constants for all our view types
    private val VIEW_TYPE_SENT_TEXT = 1
    private val VIEW_TYPE_RECEIVED_TEXT = 2
    private val VIEW_TYPE_SENT_IMAGE = 3
    private val VIEW_TYPE_RECEIVED_IMAGE = 4
    private val VIEW_TYPE_SENT_IMAGE_GROUP = 5
    private val VIEW_TYPE_RECEIVED_IMAGE_GROUP = 6

    private val auth = FirebaseAuth.getInstance()

    // --- ViewHolder Classes ---
    class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageTextView)
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageImage: ImageView = itemView.findViewById(R.id.messageImageView)
    }

    class ImageGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image1: ImageView = itemView.findViewById(R.id.image1)
        val image2: ImageView = itemView.findViewById(R.id.image2)
        val image3: ImageView = itemView.findViewById(R.id.image3)
        val image4: ImageView = itemView.findViewById(R.id.image4)
        val moreImagesText: TextView = itemView.findViewById(R.id.more_images_text)
        val image4Container: FrameLayout = itemView.findViewById(R.id.image4_container)
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = itemList[position]) {
            is DisplayableItem.MessageItem -> {
                val message = item.message
                val isSentByUser = auth.currentUser?.uid == message.senderId
                when (message.type) {
                    "IMAGE" -> if (isSentByUser) VIEW_TYPE_SENT_IMAGE else VIEW_TYPE_RECEIVED_IMAGE
                    else -> if (isSentByUser) VIEW_TYPE_SENT_TEXT else VIEW_TYPE_RECEIVED_TEXT
                }
            }
            is DisplayableItem.ImageGroup -> {
                val isSentByUser = auth.currentUser?.uid == item.messages.first().senderId
                if (isSentByUser) VIEW_TYPE_SENT_IMAGE_GROUP else VIEW_TYPE_RECEIVED_IMAGE_GROUP
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT_TEXT -> TextViewHolder(inflater.inflate(R.layout.item_chat_sent, parent, false))
            VIEW_TYPE_RECEIVED_TEXT -> TextViewHolder(inflater.inflate(R.layout.item_chat_received, parent, false))
            VIEW_TYPE_SENT_IMAGE -> ImageViewHolder(inflater.inflate(R.layout.item_chat_image_sent, parent, false))
            VIEW_TYPE_RECEIVED_IMAGE -> ImageViewHolder(inflater.inflate(R.layout.item_chat_image_received, parent, false))
            VIEW_TYPE_SENT_IMAGE_GROUP -> ImageGroupViewHolder(inflater.inflate(R.layout.item_chat_image_group_sent, parent, false))
            else -> ImageGroupViewHolder(inflater.inflate(R.layout.item_chat_image_group_received, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.bubble_animation)

        when (val item = itemList[position]) {
            is DisplayableItem.MessageItem -> {
                when (holder) {
                    is TextViewHolder -> {
                        holder.messageText.text = item.message.text
                        holder.itemView.setOnLongClickListener {
                            showOptionsMenu(holder.itemView, item.message, isImage = false)
                            true
                        }
                    }
                    is ImageViewHolder -> {
                        Glide.with(holder.itemView.context).load(item.message.imageUrl).into(holder.messageImage)
                        holder.itemView.setOnClickListener { openImageViewer(holder.itemView, item.message.imageUrl) }
                        holder.itemView.setOnLongClickListener {
                            showOptionsMenu(holder.itemView, item.message, isImage = true)
                            true
                        }
                    }
                }
            }
            is DisplayableItem.ImageGroup -> {
                val groupHolder = holder as ImageGroupViewHolder
                val messages = item.messages
                val imageViews = listOf(groupHolder.image1, groupHolder.image2, groupHolder.image3, groupHolder.image4)

                imageViews.forEach { it.visibility = View.GONE }
                groupHolder.image4Container.foreground = null
                groupHolder.moreImagesText.visibility = View.GONE

                messages.take(4).forEachIndexed { index, message ->
                    val imageView = imageViews[index]
                    imageView.visibility = View.VISIBLE
                    Glide.with(holder.itemView.context).load(message.imageUrl).into(imageView)
                    imageView.setOnClickListener { openImageViewer(holder.itemView, message.imageUrl) }
                }

                if (messages.size > 4) {
                    groupHolder.moreImagesText.visibility = View.VISIBLE
                    groupHolder.moreImagesText.text = "+${messages.size - 4}"
                }
            }
        }
    }

    override fun getItemCount() = itemList.size

    private fun openImageViewer(view: View, imageUrl: String?) {
        imageUrl?.let {
            val intent = Intent(view.context, ImageViewerActivity::class.java)
            intent.putExtra("IMAGE_URL", it)
            view.context.startActivity(intent)
        }
    }

    private fun showOptionsMenu(view: View, message: Message, isImage: Boolean) {
        val popup = PopupMenu(view.context, view)
        popup.inflate(if (isImage) R.menu.image_options_menu else R.menu.text_options_menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_save_image -> {
                    message.imageUrl?.let { saveImageToGallery(view, it) }
                    true
                }
                R.id.action_delete_for_me -> {
                    deleteForMe(message)
                    true
                }
                R.id.action_delete_for_everyone -> {
                    deleteForEveryone(message)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun saveImageToGallery(view: View, imageUrl: String) {
        Glide.with(view.context)
            .asBitmap()
            .load(imageUrl)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val resolver = view.context.contentResolver
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, "LoversConnect_${System.currentTimeMillis()}.jpg")
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/LoversConnect")
                            }
                        }
                        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        if (imageUri != null) {
                            resolver.openOutputStream(imageUri)?.use { outputStream ->
                                resource.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(view.context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            })
    }

    private fun deleteForMe(message: Message) {
        val dbRef = FirebaseDatabase.getInstance().reference
        val currentUserId = auth.currentUser?.uid
        message.id?.let { messageId ->
            if (currentUserId != null) {
                dbRef.child("chats").child(messageId).child("deletedFor").child(currentUserId).setValue(true)
            }
        }
    }

    private fun deleteForEveryone(message: Message) {
        val dbRef = FirebaseDatabase.getInstance().reference
        message.id?.let { messageId ->
            dbRef.child("chats").child(messageId).removeValue()
        }
    }
}