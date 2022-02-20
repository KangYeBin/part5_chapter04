package chatlist

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import chatdetail.ChatRoomActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yb.part3_chapter06.DBKey.Companion.CHILD_CHAT
import com.yb.part3_chapter06.DBKey.Companion.DB_USERS
import com.yb.part3_chapter06.R
import com.yb.part3_chapter06.databinding.FragmentChatListBinding

class ChatListFragment : Fragment(R.layout.fragment_chat_list) {

    private var binding: FragmentChatListBinding? = null
    private val auth: FirebaseAuth by lazy { Firebase.auth }
    private val chatRoomList = mutableListOf<ChatListModel>()
    private lateinit var chatListAdapter: ChatListAdapter
    private lateinit var chatDB: DatabaseReference

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentChatListBinding = FragmentChatListBinding.bind(view)
        binding = fragmentChatListBinding

        chatListAdapter = ChatListAdapter(onItemClicked = {
            //채팅방 띄우기
            startActivity(Intent(context, ChatRoomActivity::class.java).putExtra("chatKey", it.key))


        })
        chatRoomList.clear()

        fragmentChatListBinding.chatListRecyclerView.adapter = chatListAdapter
        fragmentChatListBinding.chatListRecyclerView.layoutManager = LinearLayoutManager(context)

        if (auth.currentUser == null) {
            return
        }

        chatDB = Firebase.database.reference.child(DB_USERS).child(auth.currentUser?.uid.toString()).child(CHILD_CHAT)
        chatDB.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val chatListModel = it.getValue(ChatListModel::class.java)
                    chatListModel ?: return
                    chatRoomList.add(chatListModel)
                }
                chatListAdapter.submitList(chatRoomList)
                chatListAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    override fun onResume() {
        super.onResume()
        chatListAdapter.notifyDataSetChanged()
    }

}