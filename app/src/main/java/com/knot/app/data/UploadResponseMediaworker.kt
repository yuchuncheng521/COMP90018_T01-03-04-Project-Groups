package com.knot.app.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Step 2 of saving an activity response: uploads the photo/video/audio in the background
 * and attaches the (encrypted) URLs to the response document that was already saved with
 * its text. WorkManager keeps it alive if the app closes and waits for a connection.
 */
class UploadResponseMediaWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val responseId = inputData.getString("responseId") ?: return Result.failure()
        val groupId = inputData.getString("groupId") ?: return Result.failure()
        val activityId = inputData.getString("activityId") ?: return Result.failure()
        val repo = ActivitiesRepository()

        return try {
            repo.uploadResponseMedia(
                applicationContext,
                responseId,
                groupId,
                activityId,
                inputData.getString("photoPath"),
                inputData.getString("videoPath"),
                inputData.getString("audioPath")
            )
            Result.success()
        } catch (e: Exception) {
            Log.e("UploadMediaWorker", "Upload failed (attempt ${runAttemptCount + 1})", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                runCatching { repo.markMediaFailed(responseId) }
                Result.failure()
            }
        }
    }
}