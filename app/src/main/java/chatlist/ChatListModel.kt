package chatlist

data class ChatListModel(
    val buyerId: String?,
    val sellerId: String,
    val itemTitle: String,
    val key: Long
) {
    constructor() : this("", "", "", 0)
}
