package com.arnav.loversconnect

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*


class RemindersActivity : AppCompatActivity() {
    private lateinit var dbRef: DatabaseReference
    private lateinit var remindersRecyclerView: RecyclerView
    private val reminderList = ArrayList<Reminder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        remindersRecyclerView = findViewById(R.id.remindersRecyclerView)
        remindersRecyclerView.layoutManager = LinearLayoutManager(this)

        dbRef = FirebaseDatabase.getInstance().getReference("reminders")
        loadReminders()
    }

    private fun loadReminders() {
        dbRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reminderList.clear()
                for (postSnapshot in snapshot.children) {
                    val reminder = postSnapshot.getValue(Reminder::class.java)
                    if (reminder != null) {
                        reminderList.add(reminder)
                    }
                }
                remindersRecyclerView.adapter = ReminderAdapter(reminderList)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}