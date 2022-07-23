package com.yb.part5_chapter04.photo

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.yb.part5_chapter04.databinding.ViewholderImageBinding
import com.yb.part5_chapter04.extensions.loadCenterCrop

class ImageViewPagerAdapter(var uriList: MutableList<Uri>) :
    RecyclerView.Adapter<ImageViewPagerAdapter.ImageViewHolder>() {


    class ImageViewHolder(private val binding: ViewholderImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindData(uri: Uri) = with(binding) {
            imageView.loadCenterCrop(uri.toString())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding =
            ViewholderImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bindData(uriList[position])
    }

    override fun getItemCount(): Int = uriList.size


}