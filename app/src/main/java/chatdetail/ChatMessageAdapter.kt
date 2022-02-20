package chatdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yb.part3_chapter06.databinding.ItemChatMessageBinding

class ChatMessageAdapter : ListAdapter<ChatMessageModel, ChatMessageAdapter.ViewHolder>(diffUtil) {

    inner class ViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(chatMessageModel: ChatMessageModel) {
            binding.senderTextView.text = chatMessageModel.senderId
            binding.messageTextView.text = chatMessageModel.message
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }



    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<ChatMessageModel>() {
            override fun areItemsTheSame(
                oldItem: ChatMessageModel,
                newItem: ChatMessageModel,
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: ChatMessageModel,
                newItem: ChatMessageModel,
            ): Boolean {
                return oldItem == newItem
            }

        }
    }

}
