package com.example.organizemedicine.model

data class Post(
    val email: String,
    val comment: String,
    val downloadUrl: String,
    val score: Float // Assuming scores can have half-points
)
