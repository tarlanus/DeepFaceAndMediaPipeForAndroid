package com.tarlanus.deepfaceforandroid.domain

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.tarlanus.deepfaceforandroid.utils.FaceEmbedder
import com.tarlanus.deepfaceforandroid.utils.ImageProcessors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.ImageProcessor
import javax.inject.Inject
@OptIn(ExperimentalGetImage::class)
class UseCaseFaceComparator @Inject constructor(
    private val useCaseStatus: UseCaseStatus,
    @ApplicationContext private val context: Context
) : ImageAnalysis.Analyzer {

    private var isFaceAccepted = false
    private var isFinised = false

    var candidateBitmapfortest : Bitmap? = null

    fun setCandidateBitmap(canidateImage : Bitmap) {
        candidateBitmapfortest = canidateImage
        useCaseStatus.setRecognitionGlobal(null)

    }
    fun clearFinised() {
        useCaseStatus.setAnotherFace(false)

        useCaseStatus.setRecognitionGlobal(null)

        isFinised = false
    }
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .build()

    private val faceDetector = FaceDetection.getClient(
        options
    )

    private val imageProcessors = ImageProcessors()
    val baseOptions = BaseOptions.builder()

        .setModelAssetPath("gesture_recognizer.task")
        .setDelegate(Delegate.CPU)
        .build()
    val gestureRecognizer =
        GestureRecognizer.createFromOptions(
            context,
            GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.7f)
                .setMinTrackingConfidence(0.7f)
                .setMinHandPresenceConfidence(0.7f)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, _ ->
                    processGesture(result)
                }

                .build()
        )

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val exceptionHandler = CoroutineExceptionHandler {_, throwable ->
            Log.e("getThrowableWhille", "${throwable.message}")
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    Log.e("FaceAnalyzer", "Detected ${faces.size} face(s)")

                    val bitmap = imageProxy.toBitmap()
                    val face = faces.first()


                    Log.e("FaceQuality", "Face GOOD")
                    useCaseStatus.setCaptureStatus(true)
                    val proxyBitmap = imageProxy.toBitmap()
                    val rotated = imageProcessors.getRotated(proxyBitmap, imageProxy.imageInfo.rotationDegrees)

                    val getCropped = imageProcessors.cropAndAlign(rotated, face)
                    if (getCropped != null) {
                        CoroutineScope(Dispatchers.Default + exceptionHandler).launch {
                            val mpImage: MPImage = BitmapImageBuilder(rotated).build()
                            gestureRecognizer.recognizeAsync(mpImage, System.currentTimeMillis())
                            compareFaces(getCropped)

                        }
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

    private fun processGesture(
        result: GestureRecognizerResult,
    ) {
        Log.e("UseCaseCompareFaces", "isFinised $isFinised")


        if (isFaceAccepted == false) {

            return
        }
        if (isFinised == true) {
            return
        }



        val gestureList = result.gestures().firstOrNull() ?: return
        val topGesture = gestureList.firstOrNull() ?: return

        val catName = topGesture.categoryName()
        val score = topGesture.score()
        Log.e("UseCaseCompareFaces", "catscore $score")


        Log.e("UseCaseCompareFaces", "catname$catName")
        Log.e("UseCaseCompareFaces", "catnameindexed${catName[0]}")


        val gestureName = topGesture.categoryName()
        val confidence = topGesture.score()
        Log.e("UseCaseCompareFaces", "Gesture: $gestureName (Confidence: ${"%.2f".format(confidence)})")


        if (gestureName == "Thumb_Up") {
            isFinised = true
            useCaseStatus.setRecognitionGlobal(true)


        }




    }

    private fun compareFaces(cameraBitmap: Bitmap) {




        val embedder = FaceEmbedder(context)

        val getMediaBitmap = candidateBitmapfortest
        if (getMediaBitmap != null) {
            val emb1 = embedder.l2Normalize(embedder.getEmbedding(cameraBitmap))
            val emb2 = embedder.l2Normalize(embedder.getEmbedding(getMediaBitmap))

            val similarity = embedder.cosineSimilarity(emb1, emb2)

            useCaseStatus.setComparisonStatus(similarity.toString())

            val verified = similarity >= 0.82f
            if (verified) {
                isFaceAccepted = true
            } else {
                isFaceAccepted = false
                useCaseStatus.setAnotherFace(true)

            }
            Log.e("UseCaseCompareFaces", "similarity = $similarity")
        }


    }



}