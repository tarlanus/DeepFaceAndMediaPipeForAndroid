package com.tarlanus.deepfaceforandroid.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.media.ExifInterface
import android.util.Log
import com.google.mlkit.vision.face.Face
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

class ImageProcessors  {
    fun isFaceHighQuality(face: Face, width: Int, height: Int): Boolean {
        val box = face.boundingBox
        val faceWidthRatio = box.width().toFloat() / width
        val faceHeightRatio = box.height().toFloat() / height
        if (faceWidthRatio < 0.35f || faceHeightRatio < 0.35f) return false
        val centerX = box.centerX()
        val centerY = box.centerY()
        val imageCenterX = width / 2f
        val imageCenterY = height / 2f
        if (abs(centerX - imageCenterX) > width * 0.20f) return false
        if (abs(centerY - imageCenterY) > height * 0.20f) return false

        if (abs(face.headEulerAngleY) > 15) return false
        if (abs(face.headEulerAngleX) > 15) return false
        if (abs(face.headEulerAngleZ) > 15) return false
        return true
    }

    fun loadCorrectlyOrientedBitmap(path: String): Bitmap? {
        val bitmap = decodeBitmapForFaceDetection(path) ?: return null

        val exif = ExifInterface(path)
        val rotation = when (
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        if (rotation == 0) return bitmap

        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )

        bitmap.recycle()
        return rotated
    }
    fun decodeBitmapForFaceDetection(
        path: String,
        maxSize: Int = 1024
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        var inSampleSize = 1
        while (options.outWidth / inSampleSize > maxSize ||
            options.outHeight / inSampleSize > maxSize
        ) {
            inSampleSize *= 2
        }

        return BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )
    }



    fun Bitmap.rotate(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
    fun getRotated(getBitmap: Bitmap, rotationDegrees: Int) : Bitmap {
        val degrees = rotationDegrees
        if (degrees == 0) return getBitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(getBitmap, 0, 0, getBitmap.width, getBitmap.height, matrix, true)
    }




    fun cropTo160(original: Bitmap, rect: Rect): Bitmap? {
        return try {
            val x = rect.left.coerceAtLeast(0)
            val y = rect.top.coerceAtLeast(0)
            val w = rect.width().coerceAtMost(original.width - x)
            val h = rect.height().coerceAtMost(original.height - y)
            val cropped = Bitmap.createBitmap(original, x, y, w, h)
            Bitmap.createScaledBitmap(cropped, 160, 160, true)
        } catch (e: Exception) {
            Log.e("FacialAnalyser", "Crop failed: ${e.message}")
            null
        }
    }

     fun loadScaledBitmap(path: String, maxWidth: Int, maxHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        var scale = 1
        while (options.outWidth / scale > maxWidth || options.outHeight / scale > maxHeight) {
            scale *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = scale }
        return BitmapFactory.decodeFile(path, opts)
    }

    fun cropAndAlign(bitmap: Bitmap, face: Face): Bitmap {
        val rect = face.boundingBox

        val margin = 40
        val left = max(0, rect.left - margin)
        val top = max(0, rect.top - margin)
        val right = min(bitmap.width, rect.right + margin)
        val bottom = min(bitmap.height, rect.bottom + margin)

        var cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)

        val leftEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)?.position

        if (leftEye != null && rightEye != null) {
            val dy = rightEye.y - leftEye.y
            val dx = rightEye.x - leftEye.x
            val angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()

            val matrix = Matrix()
            matrix.postRotate(-angle)

            cropped = Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
        }

        return Bitmap.createScaledBitmap(cropped, 160, 160, true)
    }



}