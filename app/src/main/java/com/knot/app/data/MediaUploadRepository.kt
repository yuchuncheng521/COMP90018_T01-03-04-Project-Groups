package com.knot.app.data

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

class MediaUploadRepository {

    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadMedia(
        localFilePath: String,
        folderName: String
    ): String {
        val file = File(localFilePath)

        val storageRef = storage.reference
            .child("$folderName/${file.name}")

        storageRef
            .putFile(Uri.fromFile(file))
            .await()

        val downloadUrl = storageRef
            .downloadUrl
            .await()

        return downloadUrl.toString()
    }
}