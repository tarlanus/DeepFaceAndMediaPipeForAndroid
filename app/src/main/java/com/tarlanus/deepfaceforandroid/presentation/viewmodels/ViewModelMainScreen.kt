package com.tarlanus.deepfaceforandroid.presentation.viewmodels

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.tarlanus.deepfaceforandroid.Constants.keyManifestMedia33
import com.tarlanus.deepfaceforandroid.Constants.keyManifestReadExternal
import com.tarlanus.deepfaceforandroid.Constants.keyManifestWriteExternal
import com.tarlanus.deepfaceforandroid.domain.UseCaseFaceDetector
import com.tarlanus.deepfaceforandroid.domain.UseCaseStatus
import com.tarlanus.deepfaceforandroid.domain.UseCaseUritoImage
import com.tarlanus.deepfaceforandroid.presentation.viewstates.ViewStateImageAnalyzing
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ViewModelMainScreen @Inject constructor(
    @ApplicationContext private val context: Context,
    private val useCaseUritoImage: UseCaseUritoImage,
    private val useCaseFaceDetector: UseCaseFaceDetector,
    private val useCaseStatus: UseCaseStatus,



    ) : ViewModel() {


    val comparisonStatus = useCaseStatus.comparisonPercentage


    private val _uiState: MutableStateFlow<ViewStateImageAnalyzing> =
        MutableStateFlow(ViewStateImageAnalyzing.IDLE)

    val uiState get() = _uiState.asStateFlow()



    fun checkGallery(): Boolean {
        val buildVersion = Build.VERSION.SDK_INT
        return when {
            buildVersion >= 33 -> {
                ContextCompat.checkSelfPermission(
                    context,
                    keyManifestMedia33
                ) == PackageManager.PERMISSION_GRANTED
            }

            buildVersion > 28 -> {
                ContextCompat.checkSelfPermission(
                    context,
                    keyManifestReadExternal
                ) == PackageManager.PERMISSION_GRANTED
            }

            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    keyManifestWriteExternal
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun getPermission(): String {
        val buildVersion = Build.VERSION.SDK_INT
        return when {
            buildVersion >= 33 -> {
                keyManifestMedia33
            }

            buildVersion > 28 -> {
                keyManifestReadExternal
            }

            else -> {
                keyManifestWriteExternal
            }
        }
    }

    fun analyzImageContainsFaceOrNot(getUri: Uri) {
        val pathOfImage = useCaseUritoImage.getPath(context, getUri)
        if (pathOfImage != null) {
            analyzImageContainsFaceOrNot(pathOfImage)
        } else {
            _uiState.value = ViewStateImageAnalyzing.ERROR
        }
    }

    fun analyzImageContainsFaceOrNot(pathOfImage: String) {
        _uiState.value = ViewStateImageAnalyzing.LOADING

        useCaseFaceDetector.detectInitialFaceExistsFromPath(pathOfImage) {
            if (it != null) {
                Log.e("getFace", "$it")
                _uiState.value = ViewStateImageAnalyzing.SUCCESS(it, pathOfImage)
            } else {
                _uiState.value = ViewStateImageAnalyzing.ERROR
            }
        }
    }



}