package com.example.qahelper.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

data class PreparedImage(
    val uploadBase64: String,
    val thumbnailBase64: String,
    val width: Int,
    val height: Int
)

object ImageUtils {

    private const val UPLOAD_MAX_EDGE = 1024
    private const val UPLOAD_QUALITY = 70
    private const val THUMB_MAX_EDGE = 180
    private const val THUMB_QUALITY = 55

    fun createCameraUri(context: Context): Uri {
        val cameraDir = File(context.cacheDir, "camera")
        if (!cameraDir.exists() && !cameraDir.mkdirs()) {
            throw IOException("无法创建拍照缓存目录")
        }

        // 顺手清理一天前的临时拍照文件。
        val expireAt = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        cameraDir.listFiles()?.forEach { file ->
            if (file.lastModified() < expireAt) {
                runCatching { file.delete() }
            }
        }

        val imageFile = File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    fun prepareForUpload(context: Context, uri: Uri): PreparedImage {
        val original = decodeSampledBitmap(context, uri, UPLOAD_MAX_EDGE)
            ?: throw IOException("无法读取图片，请重新选择")

        var uploadBitmap: Bitmap? = null
        var thumbBitmap: Bitmap? = null
        try {
            val upload = scaleDown(original, UPLOAD_MAX_EDGE)
            uploadBitmap = upload
            val thumb = scaleDown(upload, THUMB_MAX_EDGE)
            thumbBitmap = thumb

            return PreparedImage(
                uploadBase64 = bitmapToBase64(upload, UPLOAD_QUALITY),
                thumbnailBase64 = bitmapToBase64(thumb, THUMB_QUALITY),
                width = upload.width,
                height = upload.height
            )
        } finally {
            if (thumbBitmap != null && thumbBitmap !== uploadBitmap && thumbBitmap !== original) {
                thumbBitmap.recycle()
            }
            if (uploadBitmap != null && uploadBitmap !== original) {
                uploadBitmap.recycle()
            }
            if (!original.isRecycled) {
                original.recycle()
            }
        }
    }

    fun decodePreview(context: Context, uri: Uri, maxEdge: Int = 1024): Bitmap? {
        return runCatching { decodeSampledBitmap(context, uri, maxEdge) }.getOrNull()
    }

    fun decodeThumbnail(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: throw IOException("无法打开图片")

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("图片格式不受支持")
        }

        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxEdge) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IOException("无法解码图片")

        val rotation = readRotation(context, uri)
        val oriented = if (rotation != 0f) {
            Bitmap.createBitmap(
                decoded,
                0,
                0,
                decoded.width,
                decoded.height,
                Matrix().apply { postRotate(rotation) },
                true
            ).also {
                if (it !== decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        return scaleDown(oriented, maxEdge)
    }

    private fun readRotation(context: Context, uri: Uri): Float {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
    }

    private fun scaleDown(source: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        if (longest <= maxEdge) return source

        val ratio = maxEdge.toFloat() / longest.toFloat()
        val width = (source.width * ratio).toInt().coerceAtLeast(1)
        val height = (source.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap, quality: Int): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}
