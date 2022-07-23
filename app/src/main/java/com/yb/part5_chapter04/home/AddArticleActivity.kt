package com.yb.part5_chapter04.home

import android.app.Activity
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
import com.yb.part5_chapter04.databinding.ActivityAddArticleBinding
import com.yb.part5_chapter04.photo.CameraActivity

class AddArticleActivity : AppCompatActivity() {

    companion object {
        const val PERMISSION_REQUEST_CODE = 1000
        const val GALLERY_REQUEST_CODE = 1001
        const val CAMERA_REQUEST_CODE = 1002

        const val URI_LIST_KEY = "uriList"

    }

    private lateinit var binding: ActivityAddArticleBinding
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
        binding = ActivityAddArticleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() = with(binding) {
        imageAddButton.setOnClickListener {
            showPictureUploadDialog()
        }


        submitButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val content = contentEditText.text.toString()
            val sellerId = auth.currentUser?.uid.orEmpty()

            showProgressBar()

            if (selectedUri != null) {
                //비동기 방식
                val photoUri = selectedUri ?: return@setOnClickListener
                uploadPhoto(photoUri,
                    successHandler = {
                        uploadArticle(sellerId, title, content, it)
                    },
                    errorHandler = {
                        hideProgressBar()
                        Toast.makeText(this@AddArticleActivity,
                            "사진 업로드를 실패했습니다",
                            Toast.LENGTH_SHORT).show()

                    })
            } else {
                //동기 방식
                uploadArticle(sellerId, title, content, "")
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

    private fun uploadArticle(sellerId: String, title: String, content: String, imageUrl: String) {
        val model = ArticleModel(sellerId, title, System.currentTimeMillis(), content, imageUrl)
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
                    startGalleryScreen()
                } else {
                    Toast.makeText(this, "권한을 거부하셨습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startGalleryScreen() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"

        startActivityForResult(intent, GALLERY_REQUEST_CODE)
//        startActivityForResultLauncher.launch(intent)
    }

    private fun startCameraScreen() {
        startActivityForResult(
            CameraActivity.newIntent(this), CAMERA_REQUEST_CODE
        )

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
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        when (requestCode) {
            GALLERY_REQUEST_CODE -> {
                val uri = data?.data
                if (uri != null) {
                    binding.photoImageView.setImageURI(uri)
                    selectedUri = uri
                } else {
                    Toast.makeText(this, "사진을 가져오지 못했습니다", Toast.LENGTH_SHORT).show()
                }
            }

            CAMERA_REQUEST_CODE -> {
                data?.let {  intent ->
                    val uriList = intent.getParcelableArrayListExtra<Uri>(URI_LIST_KEY)
                    uriList?.let { list ->

                    }
                }

            }

            else -> Toast.makeText(this@AddArticleActivity, "사진을 가져오지 못했습니다", Toast.LENGTH_SHORT).show()


        }
    }

    private fun checkExternalStoragePermission(uploadAction: () -> Unit) {
        when {
            //이미 권한이 부여되었는지 확인
            ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED -> {
                uploadAction()
            }
            //앱에 권한이 필요한 이유 설명
            shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                showPermissionContextPopup()
            }
            //권한 요청
            else -> {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun showPictureUploadDialog() {
        AlertDialog.Builder(this)
            .setTitle("사진 첨부")
            .setMessage("사진 첨부할 방식을 선택하세요")
            .setPositiveButton("카메라") { _, _ ->
                checkExternalStoragePermission {
                    startCameraScreen()
                }
            }
            .setNegativeButton("갤러리") { _, _ ->
                checkExternalStoragePermission {
                    startGalleryScreen()
                }
            }
            .create()
            .show()

    }
}
