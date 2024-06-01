package com.example.organizemedicine.model

data class Medicine(
    val name: String,
    val activeIngredients: String,
    val excipients: String,
    val doNotUseWith: String
)
