package com.tarlanus.deepfaceforandroid.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarlanus.deepfaceforandroid.domain.UseCaseStatus
import com.tarlanus.deepfaceforandroid.presentation.viewstates.ViewStateImageRecognition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewModelRecognitionScreen @Inject constructor(
    private val useCaseStatus: UseCaseStatus,
) : ViewModel() {


    private var jobCamera: Job? = null


    private val _uiState: MutableStateFlow<ViewStateImageRecognition> =
        MutableStateFlow(ViewStateImageRecognition.IDLE)

    val uiState get() = _uiState.asStateFlow()



    init {
        viewModelScope.launch {
            launch {
                useCaseStatus.faceRecognitionStatus.distinctUntilChanged().collect {
                    Log.e("getUseCaseStatus", "$it")

                    if (it == null) {
                        return@collect
                    }
                    if (it == true) {
                        _uiState.emit(ViewStateImageRecognition.SUCCESS)
                    }

                }
            }

        }

    }


    fun setIdleState() {
        viewModelScope.launch {
            _uiState.emit(ViewStateImageRecognition.IDLE)

        }


    }

    fun setCameraScreen() {
        Log.e("onSetCamera", "getSetCamera")
        jobCamera?.cancel()

        jobCamera = viewModelScope.launch {
            _uiState.emit(ViewStateImageRecognition.CAMERA)
            useCaseStatus.setComparisonStatus("")

            delay(29000)
            if (useCaseStatus.getGetAnotherFace() == true) {

                _uiState.emit(ViewStateImageRecognition.FAIL)

                delay(2500)
                _uiState.emit(ViewStateImageRecognition.IDLE)

            } else {
                _uiState.emit(ViewStateImageRecognition.IDLE)

            }

        }
    }


}