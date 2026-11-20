package com.cinntra.ledurecare.feature.dealers.domain

/** Whether [query] (ignoring case and surrounding spaces) appears in the dealer's name, code or city. */
fun Dealer.matches(query: String): Boolean {
    val q = query.trim()
    return q.isEmpty() ||
        name.contains(q, ignoreCase = true) ||
        bpCode.contains(q, ignoreCase = true) ||
        city.contains(q, ignoreCase = true)
}
