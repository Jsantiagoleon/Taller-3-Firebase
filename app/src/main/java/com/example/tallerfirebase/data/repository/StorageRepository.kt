package com.example.tallerfirebase.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val profilePhotosRef = storage.reference.child("profile_photos")

    suspend fun uploadProfileImageBitmap(uid: String, bitmap: Bitmap): String {
        return runCatching {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val data = baos.toByteArray()
            val ref = profilePhotosRef.child("$uid.jpg")
            val uploadTask = ref.putBytes(data).await()
            uploadTask.storage.downloadUrl.await().toString()
        }.getOrElse {
            bitmapToBase64(bitmap)
        }
    }

    suspend fun uploadProfileImageUri(context: Context, uid: String, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("No se pudo leer la imagen seleccionada")

        return runCatching {
            val ref = profilePhotosRef.child("$uid.jpg")
            val uploadTask = ref.putBytes(bytes).await()
            uploadTask.storage.downloadUrl.await().toString()
        }.getOrElse {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                bitmapToBase64(bitmap)
            } else {
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:image/jpeg;base64,$base64"
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val bytes = baos.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }
}
