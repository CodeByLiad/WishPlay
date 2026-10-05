package com.nuvetrix.wishplay.data.local.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class PassphraseManager(private val context: Context) {

    private val keyStoreAlias = "wishplay_sqlcipher_key"
    private val prefName = "wishplay_sec_prefs"
    private val prefPassphraseKey = "encrypted_db_passphrase"
    private val prefIvKey = "db_passphrase_iv"

    fun getOrCreatePassphrase(): ByteArray {
        val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(prefPassphraseKey, null)
        val ivBase64 = prefs.getString(prefIvKey, null)

        if (encryptedBase64 != null && ivBase64 != null) {
            try {
                val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                return decrypt(encrypted, iv)
            } catch (e: Exception) {
                // Fallback to regenerate if key corrupted
            }
        }

        // Generate 32 bytes (256-bit) random passphrase
        val rawPassphrase = ByteArray(32).apply {
            SecureRandom().nextBytes(this)
        }

        val (encrypted, iv) = encrypt(rawPassphrase)
        prefs.edit()
            .putString(prefPassphraseKey, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(prefIvKey, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()

        return rawPassphrase
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(keyStoreAlias)) {
            val entry = keyStore.getEntry(keyStoreAlias, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            keyStoreAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return Pair(encrypted, iv)
    }

    private fun decrypt(encrypted: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return cipher.doFinal(encrypted)
    }
}
