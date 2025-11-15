package com.tarlanus.deepfaceforandroid.presentation.viewstates

import coil3.Bitmap

sealed class ViewStateImageAnalyzing {
    object IDLE : ViewStateImageAnalyzing()
    object LOADING : ViewStateImageAnalyzing()
    object ERROR : ViewStateImageAnalyzing()
    data class SUCCESS(val imgBitmap: Bitmap, val acutalPath : String) : ViewStateImageAnalyzing()

}