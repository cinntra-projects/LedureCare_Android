package com.example.ledurecare.core.storage

/** Encrypted key-value storage for secrets (tokens, etc). Implementation is swappable. */
interface SecureStorage {
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, value: String)
    suspend fun remove(key: String)
    suspend fun clear()
}
