// java/com/arnav/loversconnect/GalleryAdapter.kt
package com.arnav.loversconnect

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GalleryAdapter(private val itemList: List<GalleryItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_IMAGE = 1

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerText: TextView = itemView.findViewById(R.id.tvHeaderDate)
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.galleryImageView)
    }

    override fun getItemViewType(position: Int): Int {
        return when (itemList[position]) {
            is GalleryItem.Header -> TYPE_HEADER
            is GalleryItem.Image -> TYPE_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_gallery_header, parent, false))
        } else {
            ImageViewHolder(inflater.inflate(R.layout.item_gallery_image, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = itemList[position]) {
            is GalleryItem.Header -> {
                (holder as HeaderViewHolder).headerText.text = item.date
            }
            is GalleryItem.Image -> {
                val imageHolder = holder as ImageViewHolder
                Glide.with(imageHolder.itemView.context).load(item.message.imageUrl).into(imageHolder.imageView)

                imageHolder.imageView.setOnClickListener {
                    val intent = Intent(holder.itemView.context, ImageViewerActivity::class.java)
                    intent.putExtra("IMAGE_URL", item.message.imageUrl)
                    holder.itemView.context.startActivity(intent)
                }
            }
        }
    }

    override fun getItemCount() = itemList.size
}