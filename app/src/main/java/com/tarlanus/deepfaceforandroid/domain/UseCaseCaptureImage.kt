package com.tarlanus.deepfaceforandroid.domain

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class UseCaseCaptureImage @Inject constructor(private val uritoImage: UseCaseUritoImage) {

    @SuppressLint("SimpleDateFormat")
    fun captureImage(context: Context, imageCapture: ImageCapture?, onResult :(String?) -> Unit) {


        var imagePath : String? = null
        val fileName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "DeepFaceAndroid")
            if (!appDir.exists()) appDir.mkdirs()

            val photoFile = File(appDir, fileName)

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            imageCapture?.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("CameraX", "Photo capture failed: ${exc.message}", exc)
                        imagePath = null
                        onResult(imagePath)

                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Log.d("CameraX", "Photo saved: ${photoFile.absolutePath}")
                        imagePath = photoFile.absolutePath
                        onResult(imagePath)
                    }
                }
            )

        } else {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/DeepFaceAndroid"
                )
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            imageCapture?.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e("CameraX", "Photo capture failed: ${exc.message}", exc)
                        imagePath = null
                        onResult(imagePath)

                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        Log.d("CameraX", "Photo saved to MediaStore: ${output.savedUri?.path}")
                        val getUri = output.savedUri
                        Log.e("getUriSaved", "uri $getUri")
                        if (getUri != null) {
                            val photoFile = uritoImage.getPath(context, getUri)
                            if (photoFile != null) {
                                Log.e("getUriSaved", "photoFile $photoFile")
                                imagePath = photoFile
                                onResult(imagePath)

                            } else {
                                Log.e("getUriSaved", "photoFile isNull")

                            }

                        }


                    }
                }
            )
        }


    }
}