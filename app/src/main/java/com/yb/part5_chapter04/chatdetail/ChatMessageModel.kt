package com.yb.part5_chapter04.chatdetail

data class ChatMessageModel(
    val senderId: String,
    val message: String,

    ) {
    constructor() : this("", "")
}
