package com.example.streetsmart

data class Report(
    val description: String,
    val location: String,
    var upVotes: Int,
    var downVotes: Int
)