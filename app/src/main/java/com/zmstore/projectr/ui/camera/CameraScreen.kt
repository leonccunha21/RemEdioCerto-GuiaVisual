package com.zmstore.projectr.ui.camera

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zmstore.projectr.ui.theme.*
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

import androidx.compose.ui.res.stringResource
import com.zmstore.projectr.R

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScreen(
    onTextDetected: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    
    var detectedText by remember { mutableStateOf("") }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    
    // Stabilization state
    var lastCandidate by remember { mutableStateOf("") }
    var recognitionCount by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    recognizer.process(image)
                                        .addOnSuccessListener { visionText ->
                                            if (visionText.text.isNotBlank()) {
                                                val allLines = visionText.textBlocks.flatMap { it.lines }
                                                
                                                val candidates = allLines
                                                    .filter { line -> 
                                                        val rect = line.boundingBox ?: return@filter false
                                                        val isVerticalCenter = rect.centerY() > image.height * 0.2 && 
                                                                              rect.centerY() < image.height * 0.8
                                                        
                                                        line.text.trim().length > 3 && 
                                                        line.text.any { it.isLetter() } &&
                                                        isVerticalCenter &&
                                                        !line.text.contains("mg", ignoreCase = true) &&
                                                        !line.text.contains("comprimidos", ignoreCase = true) &&
                                                        !line.text.contains("conteúdo", ignoreCase = true)
                                                    }
                                                    .sortedByDescending { it.boundingBox?.height() ?: 0 }
                                                
                                                val bestName = candidates.firstOrNull()?.text?.trim() ?: ""
                                                val dosageCandidate = allLines.find { it.text.contains(Regex("\\d+\\s*(mg|ml|g|mcg|UI)", RegexOption.IGNORE_CASE)) }?.text?.trim() ?: ""

                                                val fullMatch = if (dosageCandidate.isNotBlank() && bestName.isNotBlank() && !bestName.contains(dosageCandidate)) {
                                                    "$bestName $dosageCandidate"
                                                } else {
                                                    bestName
                                                }

                                                if (fullMatch.isNotBlank()) {
                                                    if (fullMatch == lastCandidate) {
                                                        recognitionCount++
                                                        if (recognitionCount >= 5) {
                                                            detectedText = fullMatch
                                                        }
                                                    } else {
                                                        lastCandidate = fullMatch
                                                        recognitionCount = 1
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageAnalyzer
                        )
                        cameraControl = camera.cameraControl
                    } catch (exc: Exception) {
                        Log.e("CameraScreen", "Use case binding failed", exc)
                    }
                }, androidx.core.content.ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        cameraControl?.let { control ->
                            val factory = SurfaceOrientedMeteringPointFactory(
                                size.width.toFloat(),
                                size.height.toFloat()
                            )
                            val point = factory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                            control.startFocusAndMetering(action)
                        }
                    }
                }
        )

        // Scanning Animation
        val infiniteTransition = rememberInfiniteTransition(label = "scanning")
        val scanAnim by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scanLine"
        )

        // Visual Overlay (Scanning Frame)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameWidth = size.width * 0.85f
            val frameHeight = size.height * 0.25f
            val left = (size.width - frameWidth) / 2
            val top = (size.height - frameHeight) / 2
            
            // Draw background dimming
            drawRect(color = Color.Black.copy(alpha = 0.7f))
            
            // Clear the center frame
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(frameWidth, frameHeight),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )
            
            // Border with gradient
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(MedicleanTeal, MedicleanMint, MedicleanTeal)
                ),
                topLeft = Offset(left, top),
                size = Size(frameWidth, frameHeight),
                style = Stroke(width = 2.dp.toPx())
            )

            // Scanning Line
            val scanLineY = top + (frameHeight * scanAnim)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, MedicleanMint, Color.Transparent)
                ),
                start = Offset(left, scanLineY),
                end = Offset(left + frameWidth, scanLineY),
                strokeWidth = 3.dp.toPx()
            )
            
            // Premium Corners
            val cornerLen = 32.dp.toPx()
            val stroke = 4.dp.toPx()
            
            // TL
            drawLine(MedicleanMint, Offset(left, top), Offset(left + cornerLen, top), stroke)
            drawLine(MedicleanMint, Offset(left, top), Offset(left, top + cornerLen), stroke)
            // TR
            drawLine(MedicleanMint, Offset(left + frameWidth, top), Offset(left + frameWidth - cornerLen, top), stroke)
            drawLine(MedicleanMint, Offset(left + frameWidth, top), Offset(left + frameWidth, top + cornerLen), stroke)
            // BL
            drawLine(MedicleanMint, Offset(left, top + frameHeight), Offset(left + cornerLen, top + frameHeight), stroke)
            drawLine(MedicleanMint, Offset(left, top + frameHeight), Offset(left, top + frameHeight - cornerLen), stroke)
            // BR
            drawLine(MedicleanMint, Offset(left + frameWidth, top + frameHeight), Offset(left + frameWidth - cornerLen, top + frameHeight), stroke)
            drawLine(MedicleanMint, Offset(left + frameWidth, top + frameHeight), Offset(left + frameWidth, top + frameHeight - cornerLen), stroke)
        }

        // Overlay UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Top Controls
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = "Fechar", 
                        tint = Color.White, 
                        modifier = Modifier.padding(12.dp).size(24.dp)
                    )
                }

                Surface(
                    onClick = {
                        isFlashOn = !isFlashOn
                        cameraControl?.enableTorch(isFlashOn)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isFlashOn) MedicleanGold.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.4f)
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (isFlashOn) Color.Black else Color.White,
                        modifier = Modifier.padding(12.dp).size(24.dp)
                    )
                }
            }

            // Bottom Info/Action
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (detectedText.isNotBlank()) {
                    Surface(
                        modifier = Modifier.padding(bottom = 20.dp),
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MEDICAMENTO IDENTIFICADO", style = MaterialTheme.typography.labelSmall, color = MedicleanTeal, fontWeight = FontWeight.Black)
                            Text(detectedText.uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MedicleanDarkGreen)
                        }
                    }

                    Button(
                        onClick = { onTextDetected(detectedText) },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedicleanTeal)
                    ) {
                        Text("CONFIRMAR MEDICAMENTO", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                } else {
                    Surface(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        Text(
                            "Aponte para o nome na embalagem",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
