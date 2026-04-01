// java/com/arnav/loversconnect/ReminderAdapter.kt
package com.arnav.loversconnect

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*
import android.view.animation.ScaleAnimation

class ReminderAdapter(private val reminderList: List<Reminder>) :
    RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvReminderTitle)
        val date: TextView = itemView.findViewById(R.id.tvReminderDate)
        val optionsButton: ImageButton = itemView.findViewById(R.id.btnReminderOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reminder, parent, false)
        return ReminderViewHolder(view)
    }

// In ReminderAdapter.kt

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminderList[position]
        holder.title.text = reminder.title
        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        holder.date.text = sdf.format(Date(reminder.timestamp))

        // Set the list entry animation
        holder.itemView.animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_animation_fall_down)

        // Set the click listener for the three-dots menu (Edit/Delete)
        holder.optionsButton.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.inflate(R.menu.reminder_options_menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit_reminder -> {
                        val intent = Intent(view.context, AddReminderActivity::class.java).apply {
                            putExtra("REMINDER_ID", reminder.id)
                            putExtra("REMINDER_TITLE", reminder.title)
                            putExtra("REMINDER_TIMESTAMP", reminder.timestamp)
                        }
                        view.context.startActivity(intent)
                        true
                    }
                    R.id.action_delete_reminder -> {
                        val dbRef = FirebaseDatabase.getInstance().getReference("reminders")
                        reminder.id?.let { dbRef.child(it).removeValue() }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // Set the click listener for the "pop" animation on the whole card
        holder.itemView.setOnClickListener {
            val anim = ScaleAnimation(
                1.0f, 0.95f, 1.0f, 0.95f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            )
            anim.duration = 100
            anim.repeatMode = ScaleAnimation.REVERSE
            anim.repeatCount = 1
            holder.itemView.startAnimation(anim)
        }
    }

    override fun getItemCount() = reminderList.size
}