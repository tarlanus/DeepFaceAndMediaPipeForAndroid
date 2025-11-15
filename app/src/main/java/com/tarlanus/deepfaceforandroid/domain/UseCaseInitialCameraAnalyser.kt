package com.tarlanus.deepfaceforandroid.domain

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import javax.inject.Inject

@OptIn(ExperimentalGetImage::class)
class UseCaseInitialCameraAnalyser @Inject constructor(private val faceDetectorUseCase: UseCaseFaceDetector,
) : ImageAnalysis.Analyzer {


    override fun analyze(imageProxy: ImageProxy) {

        faceDetectorUseCase.detectInitialFaceExitsFromCamera(imageProxy)

    }
}