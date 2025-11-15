package com.tarlanus.deepfaceforandroid.domain

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.tarlanus.deepfaceforandroid.utils.ImageProcessors
import javax.inject.Inject
import kotlin.coroutines.resume

@OptIn(ExperimentalGetImage::class)

class UseCaseFaceDetector @Inject constructor(private val useCaseStatus: UseCaseStatus) {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .build()

    private val faceDetector = FaceDetection.getClient(
        options
    )

    private val imageProcessor = ImageProcessors()

    fun detectInitialFaceExitsFromCamera(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    Log.e("FaceAnalyzer", "Detected ${faces.size} face(s)")

                    val bitmap = imageProxy.toBitmap()
                    val face = faces.first()


                    val isEnough =
                        imageProcessor.isFaceHighQuality(face, bitmap.width, bitmap.height)
                    if (isEnough) {
                        useCaseStatus.setCaptureStatus(true)

                    } else {
                        useCaseStatus.setCaptureStatus(false)

                    }


                } else {
                    Log.e("FaceAnalyzer", "No face detected")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FaceAnalyzer", "Detection failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }


    }

    fun detectInitialFaceExistsFromPath(imagePath: String, outPutCropped: (Bitmap?) -> Unit) {


        val orientedBitmap = imageProcessor.loadCorrectlyOrientedBitmap(imagePath)


        if (orientedBitmap != null) {
            val inputImage = InputImage.fromBitmap(orientedBitmap, 0)
            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces.first()
                        val cropped = imageProcessor.cropTo160(orientedBitmap, face.boundingBox)
                        outPutCropped(cropped)
                    } else {
                        outPutCropped(null)
                    }
                }
                .addOnFailureListener {
                    outPutCropped(null)
                }
        } else {
            outPutCropped(null)
        }


    }


}