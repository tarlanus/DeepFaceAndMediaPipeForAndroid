package com.tarlanus.deepfaceforandroid.presentation.viewstates

sealed class ViewStateImageRecognition {
    object CAMERA : ViewStateImageRecognition()

    object IDLE : ViewStateImageRecognition()
    object SUCCESS : ViewStateImageRecognition()
    object FAIL : ViewStateImageRecognition()

}