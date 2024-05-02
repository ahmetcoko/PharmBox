package com.example.organizemedicine.model

data class User(
    var firstName: String? = null,
    var lastName: String? = null,
    var username: String? = null,
    var phoneNumber: String? = null,
    var email: String? = null,
    var age: Int? = null,         // Assuming age is an integer value
    var height: Double? = null,   // Assuming height is a double to allow decimals
    var weight: Double? = null    // Same for weight
    // You can continue to add other fields as necessary.
)


