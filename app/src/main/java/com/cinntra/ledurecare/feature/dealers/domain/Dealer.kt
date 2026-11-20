package com.cinntra.ledurecare.feature.dealers.domain

/**
 * A dealer (business partner). [id] is what identifies it everywhere in the app, and for the real API
 * it is the dealer's `card_code`. Fields the API does not give are blank.
 */
data class Dealer(
    val id: String,
    val name: String,
    val bpCode: String,
    val city: String,
    val zone: String = "",
    val address: String = "",
    val salesPerson: String = "",
)
