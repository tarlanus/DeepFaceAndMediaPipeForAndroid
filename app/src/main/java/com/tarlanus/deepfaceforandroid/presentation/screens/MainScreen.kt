package com.tarlanus.deepfaceforandroid.presentation.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.Bitmap
import coil3.compose.AsyncImage
import com.tarlanus.deepfaceforandroid.R
import com.tarlanus.deepfaceforandroid.presentation.viewmodels.ViewModelMainScreen
import com.tarlanus.deepfaceforandroid.presentation.viewstates.ViewStateImageAnalyzing
import com.tarlanus.deepfaceforandroid.ui.theme.secondaryColor
import kotlinx.coroutines.launch


@Composable
fun MainScreen(viewModelMain: ViewModelMainScreen = hiltViewModel()) {
    val context = LocalContext.current
    val comparisonPercentage = viewModelMain.comparisonStatus.collectAsStateWithLifecycle()

    val mediaImagePath = remember { mutableStateOf<Bitmap?>(null) }

    val uiState = viewModelMain.uiState.collectAsStateWithLifecycle()
    val selectImageScreen = remember { mutableStateOf<String?>(null) }
    val imagepath = remember { mutableStateOf(R.drawable.outline_account_circle_24) }


    val showAlertDialog = remember { mutableStateOf(false) }
    val activityUriResult =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.data
            if (data != null) {
                viewModelMain.analyzImageContainsFaceOrNot(data)
            }
        }

    val galleryImageLauncer =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->

            if (result) {
                val intent = Intent(Intent.ACTION_PICK, null)
                intent.type = "image/*"
                activityUriResult.launch(input = intent)
            }


        }


    if (showAlertDialog.value == true) {
        AlertDialog(onDismissRequest = { showAlertDialog.value = false }, confirmButton = {
            TextButton(onClick = {
                showAlertDialog.value = false
                selectImageScreen.value = "Camera"
            }) { Text("Camera") }
        }, dismissButton = {
            TextButton(onClick = {
                showAlertDialog.value = false
                val isGalleryGranted = viewModelMain.checkGallery()
                if (!isGalleryGranted) {
                    galleryImageLauncer.launch(viewModelMain.getPermission())
                } else {


                    val intent = Intent(Intent.ACTION_PICK, null)
                    intent.type = "image/*"
                    activityUriResult.launch(input = intent)
                }

            }) { Text("Gallery") }
        })

    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = 0.3f))
            .safeContentPadding(), contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {


            val state = uiState.value
            when (state) {
                is ViewStateImageAnalyzing.ERROR -> {

                    Box(
                        Modifier
                            .size(250.dp)
                            .clip(shape = CircleShape)
                            .background(Color.Gray.copy(alpha = 0.6f))
                            .clickable {
                                showAlertDialog.value = !showAlertDialog.value
                            }, contentAlignment = Alignment.Center
                    ) {

                        Text(
                            textAlign = TextAlign.Center,
                            text = "It does not contain any faces",
                            fontSize = 24.sp,
                            color = Color.Black
                        )

                    }
                }

                is ViewStateImageAnalyzing.IDLE -> {
                    AsyncImage(
                        contentDescription = "Input image",
                        model = imagepath.value,
                        modifier = Modifier
                            .size(250.dp)
                            .clip(shape = CircleShape)
                            .background(Color.White)
                            .clickable {
                                showAlertDialog.value = !showAlertDialog.value
                            })

                }

                is ViewStateImageAnalyzing.LOADING -> {

                    CircularProgressIndicator(
                        color = secondaryColor,
                        modifier = Modifier.size(250.dp)
                    )


                }

                is ViewStateImageAnalyzing.SUCCESS -> {
                    val data = state.imgBitmap
                    val actualpath = state.acutalPath
                    mediaImagePath.value = data
                    if (comparisonPercentage.value.length >= 1) {
                        Text(fontSize = 18.sp, color = Color.Black, text = "Similarity : ${comparisonPercentage?.value}")
                    }


                    Spacer(modifier = Modifier.height(10.dp))

                    AsyncImage(
                        contentDescription = "Input image",
                        model = actualpath,
                        modifier = Modifier
                            .size(250.dp)
                            .clip(shape = CircleShape)
                            .background(Color.Black)
                            .clickable {
                                showAlertDialog.value = !showAlertDialog.value
                            })

                }


            }



            Box(
                Modifier
                    .size(295.dp)
                    .clip(shape = CircleShape), contentAlignment = Alignment.Center
            ) {


                RecognitionScreen(mediaImagePath.value)


            }


        }

        if (selectImageScreen.value != null) {

            if (selectImageScreen.value == "Camera") {
                Dialog(onDismissRequest = {
                    selectImageScreen.value = null

                }) {
                    val cameraModifier =  Modifier
                        .fillMaxSize()
                        .padding(horizontal = 30.dp, vertical = 120.dp)
                    CameraScreen(cameraModifier, showCapture = true, null) {
                        selectImageScreen.value = null
                        val getCapturedImagePath = it as? String

                        if (getCapturedImagePath != null) {
                            viewModelMain.analyzImageContainsFaceOrNot(getCapturedImagePath)
                        }
                    }
                }

            }

        }
    }


}


@Composable
@Preview(showBackground = true)
fun PreviewMainScreen() {
    MainScreen()
}


