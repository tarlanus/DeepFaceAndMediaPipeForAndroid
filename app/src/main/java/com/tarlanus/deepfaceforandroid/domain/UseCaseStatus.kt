package com.tarlanus.deepfaceforandroid.domain

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UseCaseStatus @Inject constructor() {

    private val _faceRecognitionStatus : MutableSharedFlow<Boolean?> = MutableSharedFlow(replay = 0, extraBufferCapacity =1)
    val faceRecognitionStatus get() = _faceRecognitionStatus.asSharedFlow()


    private val _captureStatus : MutableStateFlow<Boolean> = MutableStateFlow(false)
    val captureStatus get() = _captureStatus.asStateFlow()


    private val _comparisonPercentage : MutableStateFlow<String> = MutableStateFlow("")
    val comparisonPercentage get() =  _comparisonPercentage.asStateFlow()


    private var isFaceAnother = false
     fun setRecognitionGlobal(isRecognized: Boolean?) {
        _faceRecognitionStatus.tryEmit(isRecognized)
    }


    fun setComparisonStatus(statusPer : String) {
        _comparisonPercentage.value = statusPer
    }





    fun setAnotherFace(anotherFace :  Boolean) {
        isFaceAnother = anotherFace
    }

    fun getGetAnotherFace()  :  Boolean{
        return isFaceAnother

    }

    fun setCaptureStatus(captureStatus : Boolean) {
        _captureStatus.value = captureStatus
    }
}