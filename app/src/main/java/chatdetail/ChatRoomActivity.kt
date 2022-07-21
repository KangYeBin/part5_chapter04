package chatdetail

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yb.part5_chapter04.DBKey.Companion.DB_CHATS
import com.yb.part5_chapter04.R

class ChatRoomActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val chatMessageList = mutableListOf<ChatMessageModel>()
    private lateinit var chatMessageAdapter: ChatMessageAdapter
    private lateinit var chatMessageDB: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        val chatKey = intent.getLongExtra("chatKey", -1)
        chatMessageDB = Firebase.database.reference.child(DB_CHATS).child("$chatKey")
        chatMessageDB.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessageModel::class.java)
                chatMessage ?: return
                chatMessageList.add(chatMessage)
                chatMessageAdapter.submitList(chatMessageList)
                chatMessageAdapter.notifyDataSetChanged()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}

        })

        chatMessageAdapter = ChatMessageAdapter()
        findViewById<RecyclerView>(R.id.chatRoomRecyclerView).adapter = chatMessageAdapter
        findViewById<RecyclerView>(R.id.chatRoomRecyclerView).layoutManager =
            LinearLayoutManager(this)

        findViewById<Button>(R.id.sendButton).setOnClickListener {
            val message = findViewById<EditText>(R.id.messageEditText).text.toString()
            val chatMessage = auth.currentUser?.uid?.let { it1 -> ChatMessageModel(it1, message) }

            chatMessageDB.push().setValue(chatMessage)
            findViewById<EditText>(R.id.messageEditText).text.clear()

        }

    }
}