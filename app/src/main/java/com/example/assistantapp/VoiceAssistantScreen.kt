package com.example.assistantapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun VoiceAssistantScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var chatResponse by remember { mutableStateOf("") }
    val ttsState = remember { mutableStateOf<TextToSpeech?>(null) }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
    }

    LaunchedEffect(Unit) {
        ttsState.value = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                ttsState.value?.language = Locale.US
                ttsState.value?.setSpeechRate(1.25f)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsState.value?.stop()
            ttsState.value?.shutdown()
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
                        chatResponse = sendMessageToGeminiAI(spokenText, "")
                        ttsState.value?.speak(chatResponse, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { speechRecognizer.startListening(speechIntent) }
            override fun onError(error: Int) { speechRecognizer.startListening(speechIntent) }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            speechRecognizer.startListening(speechIntent)
        }
    }

    if (!hasAudioPermission) {
        ActivityCompat.requestPermissions(
            (context as Activity),
            arrayOf(Manifest.permission.RECORD_AUDIO),
            100
        )
        hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Voice Assistant",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasAudioPermission) "Listening... speak now" else "Waiting for microphone permission...",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (chatResponse.isNotBlank()) {
            Text(text = chatResponse, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { speechRecognizer.startListening(speechIntent) }) { Text("Start") }
            Button(onClick = { speechRecognizer.stopListening() }) { Text("Stop") }
            Button(onClick = { ttsState.value?.speak(chatResponse, TextToSpeech.QUEUE_FLUSH, null, null) }) { Text("Repeat") }
        }
    }
}


