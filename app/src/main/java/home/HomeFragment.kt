package home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import chatlist.ChatListModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yb.part5_chapter04.DBKey.Companion.CHILD_CHAT
import com.yb.part5_chapter04.DBKey.Companion.DB_ARTICLES
import com.yb.part5_chapter04.DBKey.Companion.DB_USERS
import com.yb.part5_chapter04.R
import com.yb.part5_chapter04.databinding.FragmentHomeBinding

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var articleDB: DatabaseReference
    private lateinit var userDB: DatabaseReference

    private val articleList = mutableListOf<ArticleModel>()
    private val listener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val articleModel = snapshot.getValue(ArticleModel::class.java)
            articleModel ?: return

            articleList.add(articleModel)
            articleAdapter.submitList(articleList)
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}
    }

    private var binding: FragmentHomeBinding? = null
    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentHomeBinding = FragmentHomeBinding.bind(view)
        binding = fragmentHomeBinding
        articleAdapter = ArticleAdapter(onItemClicked = {
            if (auth.currentUser != null) {
                if (auth.currentUser?.uid != it.sellerId) {
                    //채팅방 띄우기
                    val chatRoom = ChatListModel(
                        buyerId = auth.currentUser?.uid,
                        sellerId = it.sellerId,
                        itemTitle = it.title,
                        key = System.currentTimeMillis()
                    )

                    userDB.child(auth.currentUser?.uid.toString()).child(CHILD_CHAT).push()
                        .setValue(chatRoom)

                    userDB.child(it.sellerId).child(CHILD_CHAT).push()
                        .setValue(chatRoom)

                    Snackbar.make(view, "채팅방이 생성되었습니다\n채팅 탭을 확인해주세요", Snackbar.LENGTH_SHORT).show()

                } else {
                    Snackbar.make(view, "내가 올린 아이템입니다", Snackbar.LENGTH_SHORT).show()

                }
            } else {
                Snackbar.make(view, "로그인이 필요합니다", Snackbar.LENGTH_SHORT).show()
            }

        })
        fragmentHomeBinding.articleRecyclerView.layoutManager = LinearLayoutManager(context)
        fragmentHomeBinding.articleRecyclerView.adapter = articleAdapter

        fragmentHomeBinding.addFloatingButton.setOnClickListener {
            if (auth.currentUser != null) {
                startActivity(Intent(requireContext(), AddArticleActivity::class.java))
            } else {
                Snackbar.make(view, "로그인이 필요합니다", Snackbar.LENGTH_SHORT).show()
            }
        }

        articleList.clear()
        userDB = Firebase.database.reference.child(DB_USERS)
        articleDB = Firebase.database.reference.child(DB_ARTICLES)
        articleDB.addChildEventListener(listener)


    }

    override fun onResume() {
        super.onResume()
        articleAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        articleDB.removeEventListener(listener)
    }
}