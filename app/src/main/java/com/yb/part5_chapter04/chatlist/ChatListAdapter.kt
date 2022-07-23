package com.yb.part5_chapter04.chatlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yb.part5_chapter04.databinding.ItemChatListBinding

class ChatListAdapter(val onItemClicked: (ChatListModel) -> Unit) :
    ListAdapter<ChatListModel, ChatListAdapter.ViewHolder>(diffUtil) {
    inner class ViewHolder(private val binding: ItemChatListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(chatListModel: ChatListModel) {
            binding.chatRoomTitleTextView.text = chatListModel.itemTitle

            binding.root.setOnClickListener {
                onItemClicked(chatListModel)
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemChatListBinding.inflate(LayoutInflater.from(parent.context),
            parent,
            false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<ChatListModel>() {
            override fun areItemsTheSame(oldItem: ChatListModel, newItem: ChatListModel): Boolean {
                return oldItem.key == newItem.key
            }

            override fun areContentsTheSame(
                oldItem: ChatListModel,
                newItem: ChatListModel,
            ): Boolean {
                return oldItem == newItem
            }

        }
    }

}