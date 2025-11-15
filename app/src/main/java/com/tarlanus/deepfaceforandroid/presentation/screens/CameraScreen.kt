package com.tarlanus.deepfaceforandroid.presentation.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarlanus.deepfaceforandroid.Constants.keyManifestCamera
import com.tarlanus.deepfaceforandroid.presentation.viewmodels.ViewModelCamera
import com.tarlanus.deepfaceforandroid.ui.theme.secondaryColor

@Composable
fun CameraScreen(getModifier : Modifier, showCapture: Boolean, mediaImagePath : Bitmap?, viewModelCamera: ViewModelCamera? = hiltViewModel(), onClose: (Any?) -> Unit, ) {


    val iscCaptureButton = viewModelCamera?.captureButton?.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            this.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val permissionCamera =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->

            if (result) {
                viewModelCamera?.initializeCamera(context, lifecycleOwner, previewView, showCapture, mediaImagePath)

            } else {
                onClose(null)
            }
        }






    LaunchedEffect(Unit) {
        Log.e("getEffetctScreenCamera", "Unit")
        val checkCameraPermission = viewModelCamera?.checkCameraPermission()
        if (checkCameraPermission == true) {
            viewModelCamera.initializeCamera(
                context,
                lifecycleOwner,
                previewView,
                showCapture,
                mediaImagePath
            )
        } else {
            permissionCamera.launch(keyManifestCamera)
        }

    }
    DisposableEffect(Unit) { onDispose {
        viewModelCamera?.setonCleared()
    }}
    Card(
        modifier = getModifier, shape = RoundedCornerShape(25.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {

            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            if (showCapture) {
                if (iscCaptureButton?.value == true) {
                    Button(
                        onClick = {
                            viewModelCamera?.captureImage() {
                                onClose(it)
                                Log.e("getCapturation", "$it")

                            }
                        }, colors = ButtonDefaults.buttonColors(
                            containerColor = secondaryColor.copy(alpha = 0.3f),
                            contentColor = Color.Yellow
                        ), modifier = Modifier.padding(bottom = 50.dp)
                    ) {
                        Text(text = "Capture")
                    }
                }

            }

        }

    }


}

@Composable
@Preview(showBackground = true)
fun PreviewCameraScreen() {
    val modifier = Modifier.fillMaxSize()
    CameraScreen(modifier, showCapture = true ,null,onClose = {

    }, )
}


