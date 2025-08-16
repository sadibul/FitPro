package com.example.fitpro.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val PROFILE_IMAGES_DIR = "profile_images"
    
    /**
     * Copies an image from a content URI to internal storage and returns the file URI
     */
    suspend fun copyImageToInternalStorage(context: Context, sourceUri: Uri, userEmail: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Create profile images directory if it doesn't exist
                val profileImagesDir = File(context.filesDir, PROFILE_IMAGES_DIR)
                if (!profileImagesDir.exists()) {
                    profileImagesDir.mkdirs()
                }
                
                // Create a unique filename for this user
                val fileName = "profile_${userEmail.replace("@", "_").replace(".", "_")}.jpg"
                val destinationFile = File(profileImagesDir, fileName)
                
                // Copy the image
                val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
                inputStream?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                Log.d(TAG, "Image copied successfully to: ${destinationFile.absolutePath}")
                // Return file URI that works with Coil
                destinationFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Error copying image to internal storage", e)
                null
            }
        }
    }
    
    /**
     * Deletes the profile image for a specific user
     */
    suspend fun deleteUserProfileImage(context: Context, userEmail: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val profileImagesDir = File(context.filesDir, PROFILE_IMAGES_DIR)
                val fileName = "profile_${userEmail.replace("@", "_").replace(".", "_")}.jpg"
                val imageFile = File(profileImagesDir, fileName)
                
                if (imageFile.exists()) {
                    val deleted = imageFile.delete()
                    Log.d(TAG, "Profile image deleted: $deleted")
                    deleted
                } else {
                    Log.d(TAG, "Profile image file does not exist")
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting profile image", e)
                false
            }
        }
    }
    
    /**
     * Gets the file URI for a user's profile image if it exists
     */
    fun getUserProfileImageUri(context: Context, userEmail: String): String? {
        return try {
            val profileImagesDir = File(context.filesDir, PROFILE_IMAGES_DIR)
            val fileName = "profile_${userEmail.replace("@", "_").replace(".", "_")}.jpg"
            val imageFile = File(profileImagesDir, fileName)
            
            if (imageFile.exists()) {
                imageFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting profile image URI", e)
            null
        }
    }
}
