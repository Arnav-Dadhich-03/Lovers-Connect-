// java/com/arnav/loversconnect/AddReminderActivity.kt
package com.arnav.loversconnect

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.applandeo.materialcalendarview.CalendarView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AddReminderActivity : AppCompatActivity() {

    private var existingReminderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_reminder)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val etReminderTitle = findViewById<EditText>(R.id.etReminderTitle)
        val btnSaveReminder = findViewById<FloatingActionButton>(R.id.btnSaveReminder)

        // Check for existing reminder data (for editing)
        existingReminderId = intent.getStringExtra("REMINDER_ID")
        if (existingReminderId != null) {
            val title = intent.getStringExtra("REMINDER_TITLE")
            val timestamp = intent.getLongExtra("REMINDER_TIMESTAMP", 0)

            etReminderTitle.setText(title)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            calendarView.setDate(calendar)
        }


        btnSaveReminder.setOnClickListener {
            val selectedDate = calendarView.firstSelectedDate
            val title = etReminderTitle.text.toString().trim()
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (selectedDate == null || title.isEmpty() || currentUser == null) {
                Toast.makeText(this, "Please select a date and enter a title.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            selectedDate.set(Calendar.HOUR_OF_DAY, 9)
            selectedDate.set(Calendar.MINUTE, 0)
            selectedDate.set(Calendar.SECOND, 0)
            val timestamp = selectedDate.timeInMillis

            saveReminderToFirebase(title, timestamp, currentUser.uid)
        }
    }

    private fun saveReminderToFirebase(title: String, timestamp: Long, uid: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("reminders")
        val reminderId = existingReminderId ?: dbRef.push().key

        val reminder = Reminder(reminderId, title, timestamp, uid)

        if (reminderId != null) {
            dbRef.child(reminderId).setValue(reminder).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    scheduleNotification(timestamp, title)
                    Toast.makeText(this, "Reminder saved!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to save reminder", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun scheduleNotification(timestamp: Long, title: String) {
        val delay = timestamp - System.currentTimeMillis()
        if (delay > 0) {
            val data = Data.Builder().putString("REMINDER_TITLE", title).build()
            val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()
            WorkManager.getInstance(this).enqueue(workRequest)
        }
    }
}