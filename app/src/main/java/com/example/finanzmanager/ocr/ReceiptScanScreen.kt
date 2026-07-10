package com.example.finanzmanager.ocr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Vollbild-Kamerascreen zum Erfassen eines Belegs. Nimmt ein Foto auf,
 * liest den Text on-device per ML Kit aus, parst ihn ([ReceiptParser])
 * und liefert das Ergebnis via [onResult] zurück. Das eigentliche Buchen
 * geschieht anschließend im vorausgefüllten Buchungsformular.
 */
@Composable
fun ReceiptScanScreen(
    onDismiss: () -> Unit,
    onResult: (ScannedReceipt) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    // Kamera freigeben, sobald der Scan-Screen verlassen wird.
    DisposableEffect(Unit) {
        onDispose {
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            errorMessage = "Kamera konnte nicht gestartet werden."
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )

            // Scan-Rahmen als Orientierungshilfe
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.86f)
                    .fillMaxHeight(0.62f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.06f))
            )

            // Hinweistext oben
            Text(
                text = "Beleg innerhalb des Rahmens platzieren",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        } else {
            // Kein Kamerazugriff
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    if (permissionDenied)
                        "Kamerazugriff wurde abgelehnt. Bitte in den Einstellungen erlauben, um Belege zu scannen."
                    else
                        "Kamerazugriff wird benötigt, um Belege zu scannen.",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Kamera erlauben")
                }
            }
        }

        // Schließen-Button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White)
        }

        // Auslöser
        if (hasPermission) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(if (isProcessing) Color.Gray else Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.Black, strokeWidth = 3.dp)
                } else {
                    IconButton(
                        onClick = {
                            isProcessing = true
                            errorMessage = null
                            captureAndRecognize(
                                imageCapture = imageCapture,
                                executor = ContextCompat.getMainExecutor(context),
                                onText = { text ->
                                    isProcessing = false
                                    onResult(ReceiptParser.parse(text))
                                },
                                onError = {
                                    isProcessing = false
                                    errorMessage = it
                                }
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Beleg aufnehmen",
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
        }

        errorMessage?.let { msg ->
            Text(
                text = msg,
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFB91C1C))
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun captureAndRecognize(
    imageCapture: ImageCapture,
    executor: java.util.concurrent.Executor,
    onText: (String) -> Unit,
    onError: (String) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    onError("Bild konnte nicht gelesen werden.")
                    return
                }
                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        if (visionText.text.isBlank()) {
                            onError("Kein Text erkannt. Bitte erneut versuchen.")
                        } else {
                            onText(visionText.text)
                        }
                    }
                    .addOnFailureListener {
                        onError("Texterkennung fehlgeschlagen.")
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }

            override fun onError(exception: ImageCaptureException) {
                onError("Aufnahme fehlgeschlagen: ${exception.message}")
            }
        }
    )
}
