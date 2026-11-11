package com.example.qahelper.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlin.math.ceil
import kotlin.math.max

data class PreparedImage(
    val uploadBase64: String,
    val thumbnailBase64: String,
    val width: Int,
    val height: Int
)

object ImageUtils {

    private const val TAG = "QAHelperImage"
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

    /**
     * 相册 URI 可能来自云盘或系统文件选择器，临时授权有时在异步解码时已经失效。
     * 选中后立刻复制到应用缓存，后续预览、压缩和上传都只使用本地文件 URI。
     */
    fun copyToCache(context: Context, uri: Uri): Uri {
        val pickedDir = File(context.cacheDir, "picked")
        if (!pickedDir.exists() && !pickedDir.mkdirs()) {
            throw IOException("无法创建图片缓存目录")
        }

        val expireAt = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        pickedDir.listFiles()?.forEach { file ->
            if (file.lastModified() < expireAt) {
                runCatching { file.delete() }
            }
        }

        val target = File(pickedDir, "picked_${System.currentTimeMillis()}.jpg")
        var lastError: Exception? = null

        repeat(3) { attempt ->
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output, bufferSize = 64 * 1024)
                    }
                } ?: throw IOException("无法打开所选图片")

                if (target.length() <= 0L) {
                    throw IOException("所选图片内容为空")
                }
                return Uri.fromFile(target)
            } catch (error: Exception) {
                lastError = error
                if (attempt < 2) {
                    runCatching { Thread.sleep(180L * (attempt + 1)) }
                }
            }
        }

        runCatching { target.delete() }
        throw IOException("读取图片失败：${lastError?.message ?: "请重新选择"}", lastError)
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
        return decodeSampledBitmap(context, uri, maxEdge)
    }

    fun decodeThumbnail(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, maxEdge: Int): Bitmap? {
        var lastError: Throwable? = null

        try {
            return decodeWithBitmapFactory(context, uri, maxEdge)
        } catch (error: Throwable) {
            lastError = error
            Log.w(TAG, "BitmapFactory decode failed: ${error.javaClass.simpleName}")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return decodeWithImageDecoder(context, uri, maxEdge)
            } catch (error: Throwable) {
                lastError = error
                Log.w(TAG, "ImageDecoder decode failed: ${error.javaClass.simpleName}")
            }
        }

        if (lastError != null) {
            Log.e(TAG, "All image decoders failed", lastError)
        }
        return null
    }

    private fun decodeWithBitmapFactory(context: Context, uri: Uri, maxEdge: Int): Bitmap {
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
        var oriented = decoded
        if (rotation != 0f) {
            oriented = Bitmap.createBitmap(
                decoded,
                0,
                0,
                decoded.width,
                decoded.height,
                Matrix().apply { postRotate(rotation) },
                true
            )
            if (oriented !== decoded) decoded.recycle()
        }

        val scaled = scaleDown(oriented, maxEdge)
        if (scaled !== oriented) oriented.recycle()
        return scaled
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(context: Context, uri: Uri, maxEdge: Int): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longest = max(info.size.width, info.size.height)
            val sample = if (longest > maxEdge) {
                ceil(longest.toFloat() / maxEdge.toFloat()).toInt().coerceAtLeast(1)
            } else {
                1
            }
            decoder.setTargetSampleSize(sample)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
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
