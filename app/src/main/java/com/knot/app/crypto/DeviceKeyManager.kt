package com.knot.app.crypto

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.io.ByteArrayOutputStream

/**
 * Manages this device's own hybrid-encryption key pair (ECIES).
 * Private key material never leaves the device -- AndroidKeysetManager stores
 * it encrypted-at-rest using an Android Keystore-backed master key.
 */
object DeviceKeyManager {

    private const val PREF_FILE_NAME = "knot_device_keyset_prefs"
    private const val PREF_KEY_NAME = "knot_device_keyset"
    private const val MASTER_KEY_URI = "android-keystore://knot_device_master_key"
    private const val HYBRID_TEMPLATE = "ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM"

    private var keysetHandle: KeysetHandle? = null

    /** Call once at app startup, before anything else in this object is used. */
    fun init(context: Context) {
        HybridConfig.register()
        ensureKeyPairExists(context)
    }

    /** Generates a key pair the first time it's called on this device; reuses it after that. */
    @Synchronized
    fun ensureKeyPairExists(context: Context): KeysetHandle {
        keysetHandle?.let { return it }

        val manager = AndroidKeysetManager.Builder()
            .withSharedPref(context, PREF_KEY_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get(HYBRID_TEMPLATE))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()

        return manager.keysetHandle.also { keysetHandle = it }
    }

    /** This device's public key, safe to upload to Firestore -- contains no secret material. */
    fun publicKeysetBase64(context: Context): String {
        val handle = ensureKeyPairExists(context)
        val outputStream = ByteArrayOutputStream()
        handle.publicKeysetHandle.writeNoSecret(BinaryKeysetWriter.withOutputStream(outputStream))
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /** Decrypts bytes that were encrypted for this device via [encryptFor]. */
    fun decrypt(context: Context, ciphertext: ByteArray, contextInfo: ByteArray): ByteArray {
        val handle = ensureKeyPairExists(context)
        val hybridDecrypt = handle.getPrimitive(HybridDecrypt::class.java)
        return hybridDecrypt.decrypt(ciphertext, contextInfo)
    }

    /** Reads someone else's public key (Base64, from Firestore) and encrypts bytes for them. */
    fun encryptFor(publicKeyBase64: String, plaintext: ByteArray, contextInfo: ByteArray): ByteArray {
        val bytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)
        val publicHandle = KeysetHandle.readNoSecret(BinaryKeysetReader.withBytes(bytes))
        val hybridEncrypt = publicHandle.getPrimitive(HybridEncrypt::class.java)
        return hybridEncrypt.encrypt(plaintext, contextInfo)
    }
}
