package com.yb.part5_chapter04.home

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.yb.part5_chapter04.DBKey.Companion.DB_ARTICLES
import com.yb.part5_chapter04.databinding.ActivityAddArticleBinding
import com.yb.part5_chapter04.gallery.GalleryActivity
import com.yb.part5_chapter04.photo.CameraActivity
import com.yb.part5_chapter04.photo.PhotoListAdapter
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class AddArticleActivity : AppCompatActivity() {

    companion object {
        const val PERMISSION_REQUEST_CODE = 1000
        const val GALLERY_REQUEST_CODE = 1001
        const val CAMERA_REQUEST_CODE = 1002

        const val URI_LIST_KEY = "uriList"

    }

    private lateinit var binding: ActivityAddArticleBinding
    private var imageUriList: ArrayList<Uri> = arrayListOf()

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val storage: FirebaseStorage by lazy {
        Firebase.storage
    }
    private val articleDB: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_ARTICLES)
    }

    private val photoListAdapter = PhotoListAdapter { uri ->
        removePhoto(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddArticleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() = with(binding) {
        photoRecyclerView.adapter = photoListAdapter

        imageAddButton.setOnClickListener {
            showPictureUploadDialog()
        }

        submitButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val content = contentEditText.text.toString()
            val userId = auth.currentUser?.uid.orEmpty()

            showProgressBar()

            if (imageUriList.isNotEmpty()) {
                //비동기 방식
                lifecycleScope.launch {
                    val results = uploadPhoto(imageUriList)
                    afterUploadPhoto(results, title, content, userId)
                }

            } else {
                //동기 방식
                uploadArticle(userId, title, content, listOf())
            }
        }
    }

    private suspend fun uploadPhoto(uriList: List<Uri>) = withContext(Dispatchers.IO) {
        val uploadDeferred: List<Deferred<Any>> = uriList.mapIndexed { index, uri ->
            lifecycleScope.async {
                try {
                    val fileName = "image_${index}.png"
                    return@async storage.reference.child("article/photo").child(fileName)
                        .putFile(uri)
                        .await().storage.downloadUrl.await().toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@async Pair(uri, e)
                }
            }
        }

        return@withContext uploadDeferred.awaitAll()
    }

    private fun afterUploadPhoto(results: List<Any>, title: String, content: String, userId: String) {
        val errorResults = results.filterIsInstance<Pair<Uri, Exception>>()
        val successResults = results.filterIsInstance<String>()

        when {
            errorResults.isNotEmpty() && successResults.isNotEmpty() -> {
                photoUploadErrorButContinueDialog(errorResults, successResults, title, content, userId)
            }
            errorResults.isNotEmpty() && successResults.isEmpty() -> {
                uploadError()
            }
            else -> {
                uploadArticle(userId, title, content, results.filterIsInstance<String>())
            }
        }
    }

    private fun uploadArticle(
        userId: String,
        title: String,
        content: String,
        imageUrlList: List<String>,
    ) {
        val model = ArticleModel(userId, title, System.currentTimeMillis(), content, imageUrlList)
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
        startActivityForResult(GalleryActivity.newIntent(this), GALLERY_REQUEST_CODE)

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
                data?.let { intent ->
                    val uriList = intent.getParcelableArrayListExtra<Uri>(URI_LIST_KEY)
                    uriList?.let { list ->
                        imageUriList.addAll(list)
                        photoListAdapter.setPhotoList(imageUriList)
                    } ?: kotlin.run {
                        Toast.makeText(this, "사진을 가져오지 못했습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            CAMERA_REQUEST_CODE -> {
                data?.let { intent ->
                    val uriList = intent.getParcelableArrayListExtra<Uri>(URI_LIST_KEY)
                    uriList?.let { list ->
                        imageUriList.addAll(list)
                        photoListAdapter.setPhotoList(imageUriList)
                    }
                } ?: kotlin.run {
                    Toast.makeText(this, "사진을 가져오지 못했습니다", Toast.LENGTH_SHORT).show()
                }
            }

            else -> Toast.makeText(this, "사진을 가져오지 못했습니다", Toast.LENGTH_SHORT)
                .show()


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

    private fun photoUploadErrorButContinueDialog(
        errorResults: List<Pair<Uri,Exception>>,
        successResults: List<String>,
        title: String,
        content: String,
        userId: String
    ) {
        AlertDialog.Builder(this)
            .setTitle("특정 이미지 업로드 실패")
            .setMessage("업로드에 실패한 이미지가 있습니다\n 그럼에도 불구하고 업로드하시겠습니까?")
            .setPositiveButton("업로드", DialogInterface.OnClickListener { _, _ ->
                uploadArticle(userId, title, content, successResults)
            })
            .create()
            .show()
    }

    private fun uploadError() {
        Toast.makeText(this, "사진 업로드에 실패했습니다", Toast.LENGTH_SHORT).show()
        hideProgressBar()
    }

    private fun removePhoto(uri: Uri) {
        imageUriList.remove(uri)
        photoListAdapter.setPhotoList(imageUriList)

    }
}
