package com.tarlanus.deepfaceforandroid.presentation.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.tarlanus.deepfaceforandroid.R

import com.tarlanus.deepfaceforandroid.presentation.viewmodels.ViewModelRecognitionScreen
import com.tarlanus.deepfaceforandroid.presentation.viewstates.ViewStateImageRecognition
import com.tarlanus.deepfaceforandroid.ui.theme.secondaryColor
import kotlinx.coroutines.launch

@Composable
fun RecognitionScreen(mediaPath: Bitmap?, viewModelRecognitionScreen: ViewModelRecognitionScreen? = hiltViewModel()) {



    val shouldAnim = remember { mutableStateOf<Boolean?>(null) }
    val progress = animateFloatAsState(
        targetValue = if (shouldAnim.value == true) 1f else 0f,
        animationSpec = tween(if (shouldAnim.value == true) 30000 else if (shouldAnim.value == false) 5000 else 0),
        finishedListener = {
            shouldAnim.value = null

            viewModelRecognitionScreen?.setIdleState()
        }
    )
    LaunchedEffect(mediaPath) {
        shouldAnim.value = null
        viewModelRecognitionScreen?.setIdleState()

        Log.e("getMediaPath", "effect $mediaPath")

    }
    val uiState = viewModelRecognitionScreen?.uiState?.collectAsStateWithLifecycle()





    Box(
        modifier = Modifier
            .padding(15.dp)
            .clip(shape = CircleShape)
            .fillMaxSize()
            .background(Color.White)
            .clickable {
                if (!(uiState?.value is ViewStateImageRecognition.CAMERA)) {

                    shouldAnim.value = null
                    viewModelRecognitionScreen?.setCameraScreen()
                } else {
                    viewModelRecognitionScreen.setIdleState()
                    shouldAnim.value = null
                }

            }, contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.fillMaxSize(),
            progress = { progress.value },
            strokeWidth = 10.dp,

            color = (secondaryColor)
        )
        Column(Modifier.padding(10.dp).fillMaxSize(),horizontalAlignment = Alignment.CenterHorizontally) {
            val uiGetUiState = uiState?.value
            when (uiGetUiState) {
                ViewStateImageRecognition.CAMERA -> {
                    shouldAnim.value = true
                    val cameraModifier =  Modifier
                        .size(280.dp)
                        .clip(shape = CircleShape)
                        .background(Color.White)
                    CameraScreen(cameraModifier, showCapture = false, mediaPath) { }





                }
                ViewStateImageRecognition.FAIL -> {
                    AsyncImage(
                        contentDescription = "fail",
                        model = R.drawable.error_24px,
                        modifier = Modifier
                            .size(280.dp)
                            .clip(shape = CircleShape)
                            .background(Color.White)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "FAILED", fontSize = 21.sp, textAlign = TextAlign.Center, color = Color.Black)
                    shouldAnim.value = false


                }
                ViewStateImageRecognition.IDLE -> {
                    shouldAnim.value = false

                    AsyncImage(
                        contentDescription = "Input image for testing",
                        model = R.drawable.imagerecognition,
                        modifier = Modifier
                            .size(280.dp)
                            .clip(shape = CircleShape)
                            .background(Color.White)
                    )
                }
                ViewStateImageRecognition.SUCCESS -> {
                    AsyncImage(
                        contentDescription = "success",
                        model = R.drawable.check_24px,
                        modifier = Modifier
                            .size(280.dp)
                            .clip(shape = CircleShape)
                            .background(Color.White)
                    )
                    shouldAnim.value = false


                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "SUCCESS", fontSize = 21.sp, textAlign = TextAlign.Center, color = Color.Black)



                }

                null -> {


                }
            }
        }




    }

}
@Composable
@Preview(showBackground = true)
fun PreviewRecognitionScreen(){
    RecognitionScreen( null, null)
}