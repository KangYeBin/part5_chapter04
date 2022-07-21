package home

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.yb.part5_chapter04.DBKey.Companion.DB_ARTICLES
import com.yb.part5_chapter04.R

class AddArticleActivity : AppCompatActivity() {

    private var selectedUri: Uri? = null
    private lateinit var startActivityForResultLauncher: ActivityResultLauncher<Intent>

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val storage: FirebaseStorage by lazy {
        Firebase.storage
    }
    private val articleDB: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_ARTICLES)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_article)

        findViewById<Button>(R.id.imageAddButton).setOnClickListener {
            when {
                //이미 권한이 부여되었는지 확인
                ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED -> {
                    startContentProvider()
                }
                //앱에 권한이 필요한 이유 설명
                shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    showPermissionContextPopup()
                }
                //권한 요청
                else -> {
                    requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                        1000)
                }
            }
        }

        startActivityForResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    val getIntent = it?.data
                    if (getIntent != null) {
                        selectedUri = getIntent.data
                        findViewById<ImageView>(R.id.photoImageView).setImageURI(selectedUri)
                    } else {
                        Toast.makeText(this, "사진을 가져오지 못했습니다", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "사진을 가져오지 못했습니다", Toast.LENGTH_SHORT).show()
                }
            }

        findViewById<Button>(R.id.submitButton).setOnClickListener {
            val title = findViewById<EditText>(R.id.titleEditText).text.toString()
            val price = findViewById<EditText>(R.id.priceEditText).text.toString()
            val sellerId = auth.currentUser?.uid.orEmpty()

            showProgressBar()

            if (selectedUri != null) {
                //비동기 방식
                val photoUri = selectedUri ?: return@setOnClickListener
                uploadPhoto(photoUri,
                    successHandler = {
                        uploadArticle(sellerId, title, price, it)
                    },
                    errorHandler = {
                        hideProgressBar()
                        Toast.makeText(this, "사진 업로드를 실패했습니다", Toast.LENGTH_SHORT).show()

                    })
            } else {
                //동기 방식
                uploadArticle(sellerId, title, price, "")
            }
        }
    }

    private fun uploadPhoto(uri: Uri, successHandler: (String) -> Unit, errorHandler: () -> Unit) {
        val fileName = "${System.currentTimeMillis()}.png"
        storage.reference.child("article/photo").child(fileName).putFile(uri)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    storage.reference.child("article/photo")
                        .child(fileName).downloadUrl.addOnSuccessListener {
                            successHandler(it.toString())
                        }.addOnFailureListener {
                            errorHandler()
                        }

                } else {
                    errorHandler()
                }

            }

    }

    private fun uploadArticle(sellerId: String, title: String, price: String, imageUrl: String) {
        val model = ArticleModel(sellerId, title, System.currentTimeMillis(), "$price 원", imageUrl)
        articleDB.push().setValue(model)

        hideProgressBar()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1000 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startContentProvider()
                } else {
                    Toast.makeText(this, "권한을 거부하셨습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startContentProvider() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"

        startActivityForResultLauncher.launch(intent)
    }

    private fun showPermissionContextPopup() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다")
            .setMessage("사진을 가져오기 위해 권한이 필요합니다")
            .setPositiveButton("허용", DialogInterface.OnClickListener { _, _ ->
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1000)
            })
            .create()
            .show()
    }

    private fun showProgressBar() {
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
    }
}
