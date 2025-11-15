package com.tarlanus.deepfaceforandroid.presentation.viewmodels

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarlanus.deepfaceforandroid.Constants.keyManifestCamera
import com.tarlanus.deepfaceforandroid.domain.UseCaseCaptureImage
import com.tarlanus.deepfaceforandroid.domain.UseCaseFaceComparator
import com.tarlanus.deepfaceforandroid.domain.UseCaseInitialCameraAnalyser
import com.tarlanus.deepfaceforandroid.domain.UseCaseStatus
import com.tarlanus.deepfaceforandroid.domain.UseCaseUritoImage
import com.tarlanus.deepfaceforandroid.presentation.viewstates.ViewStateImageRecognition
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel

class ViewModelCamera @Inject constructor(
    private val uritoImage: UseCaseUritoImage,
    private val useCaseCaptureImage: UseCaseCaptureImage,
    private val initialCameraAnalyser: UseCaseInitialCameraAnalyser,
    private val useCaseStatus: UseCaseStatus,
    private val compareAnalyser: UseCaseFaceComparator,

    @ApplicationContext private val context: Context
) : ViewModel() {


    private var camera: Camera? = null
    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_FRONT_CAMERA)

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private lateinit var cameraExecutor: ExecutorService
    private var jobCamera: Job? = null
    private var imageCapture: ImageCapture? = null

    private val _captureButton: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val captureButton get() = _captureButton.asStateFlow()


    init {
        viewModelScope.launch {
            launch {
                useCaseStatus.faceRecognitionStatus.distinctUntilChanged().collect {

                    if (it == null) {
                        return@collect
                    }
                    if (it == true) {
                        Log.e("getUseCaseStatus", "it")
                        setonCleared()

                    }


                }

            }
            launch {
                useCaseStatus.captureStatus.collect {
                    _captureButton.value = it
                }
            }
        }

    }

    fun initializeCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        showCapture: Boolean,
        mediaImagePath: Bitmap?
    ) {

        if (mediaImagePath != null && showCapture == false) {
            viewModelScope.launch {
                compareAnalyser.setCandidateBitmap(mediaImagePath)
                compareAnalyser.clearFinised()

            }
        }
        val exhandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("getThrowAble", throwable.message.toString())

        }
        jobCamera?.cancel()

        jobCamera = viewModelScope.launch(exhandler) {
            cameraExecutor = Executors.newSingleThreadExecutor()
            cameraProvider = ProcessCameraProvider.getInstance(context).get()
            setupCamera(lifecycleOwner, previewView, context, showCapture)

        }

    }

    private fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        context: Context,
        showCapture: Boolean
    ) {
        cameraProvider?.let { provider ->
            provider.unbindAll()
            val resolutionSelector = ResolutionSelector.Builder()
                .setAllowedResolutionMode(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
            preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)

                }


            val imageAnalyzer: ImageAnalysis = if (showCapture == true)
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, initialCameraAnalyser)
                    } else ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, compareAnalyser)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            try {

                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    _cameraSelector.value,
                    preview,
                    imageAnalyzer,
                    imageCapture


                )


            } catch (e: Exception) {
                Log.e("onErrorWhileSetup", e.message.toString())

            }
        }
    }


    fun unBindCamera() {
        cameraProvider?.let { provider ->
            provider.unbindAll()
        }
    }

    fun setonCleared() {
        cameraProvider?.let { provider ->
            provider.unbindAll()
        }
        Log.e("getOncleared", "onclear")
        jobCamera?.cancel()
        cameraExecutor.shutdown()
        compareAnalyser?.clearFinised()
        useCaseStatus.setComparisonStatus("")
    }


    fun checkCameraPermission(): Boolean {
        val checkPermission = ContextCompat.checkSelfPermission(
            context,
            keyManifestCamera
        ) == PackageManager.PERMISSION_GRANTED
        return checkPermission
    }


    fun captureImage(onResult: (String?) -> Unit) {
        var getPath: String? = null

        useCaseCaptureImage.captureImage(context, imageCapture) { path ->
            if (path != null) {
                Log.e("getUriSaved", "Captured path: $path")
                getPath = path
                onResult(getPath)
            } else {
                Log.e("getUriSaved", "Capture failed")
                getPath = null
                onResult(getPath)
            }
        }


    }

}