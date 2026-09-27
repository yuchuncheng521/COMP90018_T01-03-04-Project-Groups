package com.knot.app.crypto

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.subtle.AesGcmJce
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

/**
 * Manages the shared symmetric key used to encrypt content inside one group.
 *
 * Simplified, documented scope for this project:
 * - Only the "text" field of activity responses is encrypted (photo/video/audio
 *   are still just local file paths right now, not uploaded content).
 * - No re-keying when a member leaves the group.
 * - A newly-joined member only receives the group key once the group OWNER's
 *   device opens the app and [syncMissingMemberKeys] runs.
 * - Single device per account only.
 *
 * Firestore layout: groups/{groupId}/memberKeys/{uid} -> { "wrappedKey": "<base64>" }
 * "wrappedKey" is the group's raw AES-256 key, encrypted for that member using
 * their public key via DeviceKeyManager.encryptFor(...).
 */
object GroupKeyManager {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val cache = mutableMapOf<String, ByteArray>() // groupId -> raw group key (in-memory only)

    /** Call right after a group is created. Generates the group key and wraps it for the owner. */
    suspend fun ensureGroupKeyAsCreator(context: Context, groupId: String, ownerUid: String) {
        val groupKeyBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        cache[groupId] = groupKeyBytes

        runCatching {
            val ownerPublicKey = DeviceKeyManager.publicKeysetBase64(context)
            val wrapped = DeviceKeyManager.encryptFor(ownerPublicKey, groupKeyBytes, groupId.toByteArray())

            firestore.collection("groups").document(groupId)
                .collection("memberKeys").document(ownerUid)
                .set(mapOf("wrappedKey" to Base64.encodeToString(wrapped, Base64.NO_WRAP)))
                .await()
        }
    }

    /**
     * Returns this device's copy of the group key (decrypted), or null if this
     * device doesn't have a wrapped copy yet (e.g. just joined, owner hasn't synced).
     */
    suspend fun getMyGroupKey(context: Context, groupId: String, myUid: String): ByteArray? {
        cache[groupId]?.let { return it }

        return runCatching {
            val doc = firestore.collection("groups").document(groupId)
                .collection("memberKeys").document(myUid)
                .get().await()

            val wrappedBase64 = doc.getString("wrappedKey") ?: return@runCatching null
            val wrapped = Base64.decode(wrappedBase64, Base64.NO_WRAP)

            DeviceKeyManager.decrypt(context, wrapped, groupId.toByteArray())
        }.getOrNull()?.also { cache[groupId] = it }
    }

    /**
     * Only the group owner's device should call this (e.g. when GroupsViewModel loads
     * a group this user owns). Wraps the group key for any member who doesn't have a
     * copy in Firestore yet.
     */
    suspend fun syncMissingMemberKeys(context: Context, groupId: String, memberIds: List<String>, myUid: String) {
        val groupKeyBytes = getMyGroupKey(context, groupId, myUid) ?: return

        val existingKeysSnapshot = firestore.collection("groups").document(groupId)
            .collection("memberKeys").get().await()
        val alreadyHaveKey = existingKeysSnapshot.documents.map { it.id }.toSet()

        val missing = memberIds.filterNot { it in alreadyHaveKey }
        for (uid in missing) {
            val memberDoc = firestore.collection("users").document(uid).get().await()
            val memberPublicKey = memberDoc.getString("publicKey") ?: continue

            val wrapped = DeviceKeyManager.encryptFor(memberPublicKey, groupKeyBytes, groupId.toByteArray())
            firestore.collection("groups").document(groupId)
                .collection("memberKeys").document(uid)
                .set(mapOf("wrappedKey" to Base64.encodeToString(wrapped, Base64.NO_WRAP)))
                .await()
        }
    }

    /** Encrypts a plaintext string for storage. Returns null if we don't have the group key yet. */
    suspend fun encryptText(context: Context, groupId: String, myUid: String, plaintext: String): String? {
        val keyBytes = getMyGroupKey(context, groupId, myUid) ?: return null
        val aead = AesGcmJce(keyBytes)
        val ciphertext = aead.encrypt(plaintext.toByteArray(), groupId.toByteArray())
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    /** Decrypts a string previously produced by [encryptText]. Returns null if we can't decrypt yet. */
    suspend fun decryptText(context: Context, groupId: String, myUid: String, ciphertextBase64: String): String? {
        val keyBytes = getMyGroupKey(context, groupId, myUid) ?: return null
        return runCatching {
            val aead = AesGcmJce(keyBytes)
            val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            String(aead.decrypt(ciphertext, groupId.toByteArray()))
        }.getOrNull()
    }
}
