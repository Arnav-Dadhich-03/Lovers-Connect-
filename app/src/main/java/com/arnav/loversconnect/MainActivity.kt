// java/com/arnav/loversconnect/MainActivity.kt
package com.arnav.loversconnect

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    // --- Views ---
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnAttach: ImageButton
    private lateinit var wallpaperImageView: ImageView

    // --- Firebase & Adapter ---
    private lateinit var messageAdapter: ChatAdapter
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // --- Data Lists ---
    private val displayItemList = ArrayList<DisplayableItem>()
    private val fullMessageList = ArrayList<Message>()

    // --- State Variable ---
    private var chatClearTimestamp = 0L

    private val pickChatImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data?.clipData != null) {
                val count = result.data!!.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri: Uri = result.data!!.clipData!!.getItemAt(i).uri
                    uploadImageToStorage(imageUri)
                }
            } else if (result.data?.data != null) {
                val imageUri: Uri = result.data!!.data!!
                uploadImageToStorage(imageUri)
            }
        }
    }

    private val pickWallpaperLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                contentResolver.takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                wallpaperImageView.setImageURI(imageUri)
                getSharedPreferences("WallpaperPrefs", MODE_PRIVATE).edit()
                    .putString("wallpaper_uri", imageUri.toString())
                    .apply()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        auth = FirebaseAuth.getInstance()
        dbRef = FirebaseDatabase.getInstance().reference

        wallpaperImageView = findViewById(R.id.wallpaperImageView)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnAttach = findViewById(R.id.btnAttach)

        messageAdapter = ChatAdapter(displayItemList)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        (chatRecyclerView.layoutManager as LinearLayoutManager).stackFromEnd = true
        chatRecyclerView.adapter = messageAdapter

        loadWallpaper()
        listenForTimestampChanges()
        listenForChatMessages()

        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            val senderId = auth.currentUser?.uid
            if (messageText.isNotEmpty() && senderId != null) {
                val message = Message(text = messageText, senderId = senderId, type = "TEXT")
                dbRef.child("chats").push().setValue(message)
                etMessage.setText("")
            }
        }

        btnAttach.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            pickChatImageLauncher.launch(intent)
        }
    }

    private fun uploadImageToStorage(imageUri: Uri) {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()
        MediaManager.get().upload(imageUri).callback(object : UploadCallback {
            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                val secureUrl = resultData["secure_url"] as? String
                if (secureUrl != null) {
                    sendMessageWithImage(secureUrl)
                }
            }
            override fun onError(requestId: String, error: ErrorInfo) {
                Toast.makeText(baseContext, "Upload failed: ${error.description}", Toast.LENGTH_SHORT).show()
            }
            override fun onStart(requestId: String) {}
            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
            override fun onReschedule(requestId: String, error: ErrorInfo) {}
        }).dispatch()
    }

    private fun sendMessageWithImage(imageUrl: String) {
        val senderId = auth.currentUser?.uid ?: return
        val message = Message(imageUrl = imageUrl, senderId = senderId, type = "IMAGE")
        dbRef.child("chats").push().setValue(message)
    }

    private fun listenForChatMessages() {
        dbRef.child("chats").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fullMessageList.clear()
                for (postSnapshot in snapshot.children) {
                    val message = postSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        message.id = postSnapshot.key
                        fullMessageList.add(message)
                    }
                }
                filterAndDisplayMessages()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenForTimestampChanges() {
        val currentUserId = auth.currentUser?.uid ?: return
        val userChatInfoRef = FirebaseDatabase.getInstance().getReference("user_chat_info").child(currentUserId)
        userChatInfoRef.child("chatClearedTimestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatClearTimestamp = snapshot.getValue(Long::class.java) ?: 0L
                filterAndDisplayMessages()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filterAndDisplayMessages() {
        val currentUserId = auth.currentUser?.uid ?: return
        displayItemList.clear()

        val filteredMessages = fullMessageList.filter {
            it.timestamp > chatClearTimestamp && currentUserId !in it.deletedFor.keys
        }

        var i = 0
        while (i < filteredMessages.size) {
            val currentMessage = filteredMessages[i]
            if (currentMessage.type == "IMAGE") {
                val imageGroup = mutableListOf(currentMessage)
                var j = i + 1
                while (j < filteredMessages.size &&
                    filteredMessages[j].type == "IMAGE" &&
                    filteredMessages[j].senderId == currentMessage.senderId) {
                    imageGroup.add(filteredMessages[j])
                    j++
                }

                if (imageGroup.size > 1) {
                    displayItemList.add(DisplayableItem.ImageGroup(imageGroup))
                    i = j
                } else {
                    displayItemList.add(DisplayableItem.MessageItem(currentMessage))
                    i++
                }
            } else {
                displayItemList.add(DisplayableItem.MessageItem(currentMessage))
                i++
            }
        }
        messageAdapter.notifyDataSetChanged()
        if (displayItemList.isNotEmpty()) {
            chatRecyclerView.scrollToPosition(displayItemList.size - 1)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
                return true
            }
            R.id.action_set_wallpaper -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                pickWallpaperLauncher.launch(intent)
                return true
            }
            R.id.action_wallpaper_opacity -> {
                showOpacitySliderDialog()
                return true
            }
            R.id.action_clear_chat -> {
                AlertDialog.Builder(this)
                    .setTitle("Clear Chat")
                    .setMessage("This will clear messages from your view only. Are you sure?")
                    .setPositiveButton("Yes, Clear") { _, _ ->
                        auth.currentUser?.uid?.let {
                            val userChatInfoRef = FirebaseDatabase.getInstance().getReference("user_chat_info").child(it)
                            userChatInfoRef.child("chatClearedTimestamp").setValue(System.currentTimeMillis())
                                .addOnSuccessListener { Toast.makeText(this, "Chat cleared for you", Toast.LENGTH_SHORT).show() }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return true
            }
            R.id.action_add_reminder -> {
                startActivity(Intent(this, AddReminderActivity::class.java))
                return true
            }
            R.id.action_view_reminders -> {
                startActivity(Intent(this, RemindersActivity::class.java))
                return true
            }
            R.id.action_view_gallery -> {
                startActivity(Intent(this, ChatGalleryActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // THIS FUNCTION IS NOW RESTORED
    private fun loadWallpaper() {
        val prefs = getSharedPreferences("WallpaperPrefs", MODE_PRIVATE)
        val wallpaperUriString = prefs.getString("wallpaper_uri", null)
        val opacity = prefs.getFloat("wallpaper_opacity", 0.3f)

        if (wallpaperUriString != null) {
            wallpaperImageView.setImageURI(Uri.parse(wallpaperUriString))
        }
        wallpaperImageView.alpha = opacity
    }

    // THIS FUNCTION IS NOW RESTORED
    private fun showOpacitySliderDialog() {
        val prefs = getSharedPreferences("WallpaperPrefs", MODE_PRIVATE)
        val currentOpacity = prefs.getFloat("wallpaper_opacity", 0.3f)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_opacity_slider, null)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.opacitySeekBar)
        seekBar.progress = (currentOpacity * 100).toInt()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                wallpaperImageView.alpha = progress / 100f
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                prefs.edit().putFloat("wallpaper_opacity", seekBar!!.progress / 100f).apply()
            }
        })

        AlertDialog.Builder(this)
            .setTitle("Adjust Wallpaper Opacity")
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .show()
    }
}