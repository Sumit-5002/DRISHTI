package com.example.assistantapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.rememberCoroutineScope
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.TextButton
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.rememberCoroutineScope

@Composable
fun BlindModeScreen() {
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val context = LocalContext.current
    LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var currentMode by remember { mutableStateOf("navigation") }
    var overlayText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var isAssistantMode by remember { mutableStateOf(false) }
    var sessionStarted by remember { mutableStateOf(true) } // Start session immediately
    var analysisResult by remember { mutableStateOf("") }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    var lastSpokenIndex by remember { mutableStateOf(0) }
    var lastProcessedTimestamp by remember { mutableStateOf(0L) }
    val frameInterval = 3000 // Process a frame every 3 seconds
    var navigationPaused by remember { mutableStateOf(false) }
    var isMicActive by remember { mutableStateOf(false) }
    var chatResponse by remember { mutableStateOf("") }
    var isVoiceMode by remember { mutableStateOf(false) }
    var voiceModeResult by remember { mutableStateOf("") }
    var showInstructions by remember { mutableStateOf(true) }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    // Initialize TTS with better settings for blind users
    LaunchedEffect(context) {
        tts.value = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.value?.language = Locale.US
                tts.value?.setSpeechRate(0.8f) // Slower for clarity
                tts.value?.setPitch(1.0f) // Normal pitch
            }
        }
    }

    // Welcome and instruction system
    LaunchedEffect(Unit) {
        if (tts.value != null) {
            tts.value?.speak(
                "Welcome to DRISHTI Navigation Mode. " +
                "This mode provides real-time guidance using your camera. " +
                "Tap the top of the screen to pause navigation and ask questions. " +
                "Tap the bottom to switch to text reading mode. " +
                "Tap the center to hear current status. " +
                "Navigation will start automatically in 3 seconds.",
                TextToSpeech.QUEUE_FLUSH, null, null
            )
            
            // Start navigation after 3 seconds
            kotlinx.coroutines.delay(3000)
            tts.value?.speak("Navigation started. I will describe your surroundings every 3 seconds.", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.value?.stop()
            tts.value?.shutdown()
            speechRecognizer.destroy()
        }
    }

    LaunchedEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    coroutineScope.launch {
                        chatResponse = sendMessageToGeminiAI(spokenText, analysisResult)
                        tts.value?.speak(chatResponse, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                // Restart listening on end of speech, if navigation is paused
                if (navigationPaused) {
                    speechRecognizer.startListening(speechIntent)
                }
            }
            override fun onError(error: Int) {
                // Restart listening on error, if navigation is paused
                if (navigationPaused) {
                    speechRecognizer.startListening(speechIntent)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // Effect to handle microphone activation when navigation is paused
    LaunchedEffect(navigationPaused) {
        if (navigationPaused) {
            isMicActive = true
            speechRecognizer.startListening(speechIntent)
        } else {
            isMicActive = false
            speechRecognizer.stopListening()
            // Clear chatResponse to display the analysis result when resuming navigation
            chatResponse = ""
        }
    }

    if (hasPermission) {
        if (sessionStarted) {
            if (isVoiceMode) {
                VoiceModeCamera(
                    onImageCaptured = { bitmap: Bitmap ->
                        capturedImage = bitmap
                        coroutineScope.launch {
                            voiceModeResult = ""
                            sendFrameToGemini2AI(bitmap, { partialResult ->
                                voiceModeResult += partialResult
                                tts.value?.speak(partialResult, TextToSpeech.QUEUE_ADD, null, null)
                            }, { error ->
                                // Handle error
                            })
                        }
                    },

                    cameraExecutor = cameraExecutor
                )
            } else if (!navigationPaused) {
                CameraPreviewWithAnalysis { imageProxy ->
                    val currentTimestamp = System.currentTimeMillis()
                    if (currentTimestamp - lastProcessedTimestamp >= frameInterval) {
                        coroutineScope.launch {
                            val bitmap = imageProxy.toBitmap()
                            if (bitmap != null) {
                                sendFrameToGeminiAI(bitmap, { partialResult ->
                                    analysisResult += " $partialResult"
                                    val newText = analysisResult.substring(lastSpokenIndex)
                                    tts.value?.speak(newText, TextToSpeech.QUEUE_ADD, null, null)
                                    lastSpokenIndex = analysisResult.length
                                }, { error ->
                                    // Handle error here
                                })
                                lastProcessedTimestamp = currentTimestamp
                            }
                            imageProxy.close()
                        }
                    } else {
                        imageProxy.close()
                    }
                }
            }
        }
    } else {
        ActivityCompat.requestPermissions(
            (context as Activity),
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            1
        )
    }

    // Main UI with simple, accessible controls
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val screenHeight = size.height
                        val y = offset.y
                        
                        when {
                            y < screenHeight * 0.33 -> {
                                // Top third - Toggle Assistant Mode
                                if (!isVoiceMode) {
                                    navigationPaused = !navigationPaused
                                    isAssistantMode = navigationPaused
                                    
                                    // Haptic feedback
                                    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(100)
                                    }
                                    
                                    if (navigationPaused) {
                                        tts.value?.stop()
                                        currentMode = "assistant"
                                        overlayText = ""
                                        tts.value?.speak("Assistant mode activated. Ask me anything about your environment.", TextToSpeech.QUEUE_FLUSH, null, null)
                                    } else {
                                        tts.value?.stop()
                                        currentMode = "navigation"
                                        overlayText = ""
                                        chatResponse = ""
                                        tts.value?.speak("Assistant mode deactivated. Navigation resumed.", TextToSpeech.QUEUE_FLUSH, null, null)
                                    }
                                }
                            }
                            y > screenHeight * 0.66 -> {
                                // Bottom third - Toggle Voice Mode
                                if (!isAssistantMode) {
                                    isVoiceMode = !isVoiceMode
                                    
                                    // Haptic feedback
                                    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(200)
                                    }
                                    
                                    if (isVoiceMode) {
                                        tts.value?.stop()
                                        currentMode = "voice"
                                        overlayText = ""
                                        navigationPaused = true
                                        tts.value?.speak("Voice mode activated. Point your camera at text to read it.", TextToSpeech.QUEUE_FLUSH, null, null)
                                    } else {
                                        tts.value?.stop()
                                        currentMode = "navigation"
                                        overlayText = ""
                                        voiceModeResult = ""
                                        navigationPaused = false
                                        tts.value?.speak("Voice mode deactivated. Navigation resumed.", TextToSpeech.QUEUE_FLUSH, null, null)
                                    }
                                }
                            }
                            else -> {
                                // Middle third - Status and instructions
                                val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(50)
                                }
                                
                                tts.value?.speak(
                                    "Current mode: $currentMode. " +
                                    "Top tap: Assistant mode. Bottom tap: Voice mode. " +
                                    "Navigation is ${if (navigationPaused) "paused" else "active"}.",
                                    TextToSpeech.QUEUE_FLUSH, null, null
                                )
                            }
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (sessionStarted) {
                AIResponseOverlay(
                    currentMode = currentMode,
                    navigationResponse = analysisResult,
                    response = analysisResult,
                    chatResponse = chatResponse,
                    readingModeResult = voiceModeResult,
                    tts = tts.value,
                    lastSpokenIndex = lastSpokenIndex
                )
            }
            // Simple visual indicators (minimal for blind users)
            if (isVoiceMode) {
                Icon(
                    imageVector = Icons.Filled.Book,
                    contentDescription = "Voice Mode Active",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp),
                    tint = Color.Green
                )
            }
            
            if (isMicActive) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Microphone Active",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(48.dp),
                    tint = Color.Green
                )
            }
            
            // Instructions overlay
            if (showInstructions) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DRISHTI Navigation Controls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Tap TOP: Assistant Mode (ask questions)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Tap CENTER: Status & Instructions",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Tap BOTTOM: Voice Mode (read text)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showInstructions = false }
                        ) {
                            Text("Got it!")
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun VoiceModeCamera(
    onImageCaptured: (Bitmap) -> Unit,
    cameraExecutor: ExecutorService
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)

        // Capture image once when voice mode is activated
        val outputOptions = ImageCapture.OutputFileOptions.Builder(createTempFile(context.toString())).build()
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: return
                    val bitmap = BitmapFactory.decodeFile(savedUri.path)
                    onImageCaptured(bitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    // Handle error
                }
            }
        )
    }

    AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
fun ReadingModeScreen() {
    val context = LocalContext.current
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var readingResult by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    
    // Initialize TTS
    LaunchedEffect(Unit) {
        tts.value = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.value?.language = Locale.US
                tts.value?.setSpeechRate(0.8f)
                tts.value?.setPitch(1.0f)
            }
        }
        
        // Welcome message
        tts.value?.speak(
            "Welcome to DRISHTI Reading Mode. " +
            "Point your camera at any text you want to read. " +
            "I will automatically capture and read the text aloud. " +
            "Tap the screen to capture text.",
            TextToSpeech.QUEUE_FLUSH, null, null
        )
    }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            tts.value?.stop()
            tts.value?.shutdown()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                if (!isProcessing) {
                    isProcessing = true
                    tts.value?.speak("Capturing text. Please wait.", TextToSpeech.QUEUE_FLUSH, null, null)
                    
                    // Simulate text capture and reading
                    // In real implementation, this would use the camera
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        kotlinx.coroutines.delay(2000)
                        readingResult = "Sample text: Welcome to DRISHTI. This is a demonstration of the reading mode."
                        tts.value?.speak(readingResult, TextToSpeech.QUEUE_FLUSH, null, null)
                        isProcessing = false
                    }
                }
            }
    ) {
        // Camera preview would go here
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Placeholder for camera
            Text(
                text = "Camera Preview\nTap to capture text",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }
        
        // Reading result overlay
        if (readingResult.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Text Read:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = readingResult,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { 
                            readingResult = ""
                            tts.value?.speak("Ready to read more text. Tap the screen to capture.", TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    ) {
                        Text("Read More Text")
                    }
                }
            }
        }
        
        // Processing indicator
        if (isProcessing) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Processing Text...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please wait while I read the text",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

