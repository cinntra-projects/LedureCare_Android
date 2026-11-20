package com.cinntra.ledurecare.core.storage

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cinntra.ledurecare.core.common.IoDispatcher
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.secureDataStore by preferencesDataStore(name = "secure_store")

/**
 * DataStore holding AES-256-GCM ciphertext. The Tink keyset is wrapped by a key in Android Keystore.
 * The entry key is used as associated data, so a value cannot be moved to another key.
 */
@Singleton
class TinkSecureStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SecureStorage {

    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    override suspend fun getString(key: String): String? = withContext(io) {
        val encoded = context.secureDataStore.data.first()[stringPreferencesKey(key)]
            ?: return@withContext null
        runCatching {
            val plain = aead.decrypt(Base64.decode(encoded, Base64.NO_WRAP), key.toByteArray())
            String(plain)
        }.getOrNull()
    }

    override suspend fun putString(key: String, value: String) = withContext(io) {
        val cipher = aead.encrypt(value.toByteArray(), key.toByteArray())
        context.secureDataStore.edit {
            it[stringPreferencesKey(key)] = Base64.encodeToString(cipher, Base64.NO_WRAP)
        }
        Unit
    }

    override suspend fun remove(key: String) {
        context.secureDataStore.edit { it.remove(stringPreferencesKey(key)) }
    }

    override suspend fun clear() {
        context.secureDataStore.edit { it.clear() }
    }

    private companion object {
        const val KEYSET_NAME = "ledurecare_keyset"
        const val KEYSET_PREFS = "ledurecare_keyset_prefs"
        const val MASTER_KEY_URI = "android-keystore://ledurecare_master_key"
    }
}
