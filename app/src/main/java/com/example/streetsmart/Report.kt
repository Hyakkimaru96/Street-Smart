package com.example.streetsmart
import java.io.Serializable


data class Report(
    var id: String = "",
    val description: String = "",
    val location: String = "",
    var upvotes: Int = 0,
    var downvotes: Int = 0,
    val status: Int = 0,
    var userVoteStatus: Int = 0,
    val imageUrl: String = "",
    val userId: String = "" // who posted the report
) : Serializable

