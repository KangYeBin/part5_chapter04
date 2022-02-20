package chatdetail

data class ChatMessageModel(
    val senderId: String,
    val message: String,

    ) {
    constructor() : this("", "")
}
