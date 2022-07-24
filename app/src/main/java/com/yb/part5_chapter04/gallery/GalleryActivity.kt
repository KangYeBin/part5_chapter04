package com.yb.part5_chapter04.gallery

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.yb.part5_chapter04.databinding.ActivityGalleryBinding

class GalleryActivity : AppCompatActivity() {

    companion object {

        fun newIntent(activity: Activity) = Intent(activity, GalleryActivity::class.java)

        private const val URI_LIST_KEY = "uriList"
    }

    private val viewModel by viewModels<GalleryViewModel>()

    private val adapter = GalleryPhotoListAdapter { photo ->
        viewModel.selectPhoto(photo)
    }

    private lateinit var binding: ActivityGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.fetchData()
        initViews()
        observeState()
    }

    private fun initViews() = with(binding) {
        recyclerView.adapter = adapter
        confirmButton.setOnClickListener {
            viewModel.confirmCheckedPhotos()
        }
    }

    private fun observeState() = viewModel.galleryStateLiveData.observe(this) { state ->
        when (state) {
            is GalleryState.Loading -> handleLoading()
            is GalleryState.Success -> handleSuccess(state)
            is GalleryState.Confirm -> handleConfirm(state)
            else -> Unit
        }
    }

    private fun handleLoading() = with(binding) {
        progressbar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun handleSuccess(state: GalleryState.Success) = with(binding) {
        progressbar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        adapter.setPhotoList(state.photoList)
    }

    private fun handleConfirm(state: GalleryState.Confirm) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(URI_LIST_KEY, ArrayList(state.photoList.map { it.uri }))
        })
        finish()
    }

}