package com.example.assistantapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.ui.graphics.vector.ImageVector
import android.speech.tts.TextToSpeech
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(navController: NavHostController) {
    var showDeveloperInfo by remember { mutableStateOf(false) }
    var isFirstTime by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }

    // Initialize TTS
    LaunchedEffect(Unit) {
        tts.value = TextToSpeech(context) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.value?.language = Locale.US
                tts.value?.setSpeechRate(0.9f) // Slower for clarity
            }
        }
    }

    // Welcome message for first-time users
    LaunchedEffect(isFirstTime) {
        if (isFirstTime && tts.value != null) {
            tts.value?.speak(
                "Welcome to DRISHTI, your AI vision assistant. " +
                "This app has three main modes: Navigation Mode for real-time guidance and asking questions, " +
                "Reading Mode for reading text from signs and documents, and Help Tutorial to learn how to use the app. " +
                "Tap anywhere on the screen to hear options again.",
                TextToSpeech.QUEUE_FLUSH, null, null
            )
            isFirstTime = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable {
                // Audio instructions on tap
                tts.value?.speak(
                    "DRISHTI has three modes. Navigation Mode: Tap the top button. " +
                    "Voice Assistant: Tap the middle button. Help Tutorial: Tap the bottom button. " +
                    "Double tap any button to activate it.",
                    TextToSpeech.QUEUE_FLUSH, null, null
                )
            }
    ) {
        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Title with Audio Description
            Text(
                text = "DRISHTI",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "AI VISION FOR THE BLIND",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Your AI-powered vision assistant for the blind",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Accessible Feature Buttons
            AccessibleFeatureButton(
                icon = Icons.Default.CameraAlt,
                title = "Navigation Mode",
                description = "Real-time camera navigation with AI assistance and voice commands",
                audioDescription = "Navigation Mode. Double tap to activate. Provides real-time guidance and allows you to ask questions about your environment.",
                onClick = { 
                    tts.value?.speak("Opening Navigation Mode", TextToSpeech.QUEUE_FLUSH, null, null)
                    navController.navigate("blindMode") 
                },
                onDoubleClick = {
                    tts.value?.speak("Navigation Mode activated", TextToSpeech.QUEUE_FLUSH, null, null)
                    navController.navigate("blindMode")
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AccessibleFeatureButton(
                icon = Icons.Default.Book,
                title = "Reading Mode",
                description = "Read text from signs, documents, and books",
                audioDescription = "Reading Mode. Double tap to activate. Point your camera at text to read it aloud.",
                onClick = { 
                    tts.value?.speak("Opening Reading Mode", TextToSpeech.QUEUE_FLUSH, null, null)
                    navController.navigate("readingMode") 
                },
                onDoubleClick = {
                    tts.value?.speak("Reading Mode activated", TextToSpeech.QUEUE_FLUSH, null, null)
                    navController.navigate("readingMode")
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AccessibleFeatureButton(
                icon = Icons.Default.Help,
                title = "Help & Tutorial",
                description = "Learn how to use the app effectively",
                audioDescription = "Help and Tutorial. Double tap to activate. Learn how to use DRISHTI effectively.",
                onClick = { 
                    tts.value?.speak("Opening Help Tutorial", TextToSpeech.QUEUE_FLUSH, null, null)
                    // TODO: Implement help system 
                },
                onDoubleClick = {
                    tts.value?.speak("Help Tutorial activated", TextToSpeech.QUEUE_FLUSH, null, null)
                    // TODO: Implement help system
                }
            )
        }

        // Developer Info Button (Top Right)
        IconButton(
            onClick = { 
                showDeveloperInfo = !showDeveloperInfo
                tts.value?.speak(
                    if (showDeveloperInfo) "Developer information shown" else "Developer information hidden",
                    TextToSpeech.QUEUE_FLUSH, null, null
                )
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Developer Info",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Developer Info Card
        AnimatedVisibility(
            visible = showDeveloperInfo,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
        ) {
            Card(
                modifier = Modifier.width(280.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Developer Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Team 36 - VIT Bhopal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Team members:",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "SUMIT PRASAD",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "SUJEET GUPTA",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "ADVAY BHAGAT",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "KUMAR AMAN",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "KRISHANU DAS",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Help Tutorial Overlay
        var showHelpTutorial by remember { mutableStateOf(false) }
        LaunchedEffect(showHelpTutorial) {
            if (showHelpTutorial && tts.value != null) {
                tts.value?.speak(
                    "Welcome to DRISHTI! Here's how to use your AI vision assistant:",
                    TextToSpeech.QUEUE_FLUSH, null, null
                )
            }
        }
        AnimatedVisibility(
            visible = showHelpTutorial,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DRISHTI Help & Tutorial",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Welcome to DRISHTI! Here's how to use your AI vision assistant:",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    HelpSection(
                        title = "Navigation Mode",
                        description = "Real-time guidance using your camera. The app describes your surroundings every 3 seconds and allows you to ask questions.",
                        instructions = "• Point your camera forward\n• Listen to audio descriptions\n• Tap top to ask questions\n• Get obstacle warnings"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HelpSection(
                        title = "Reading Mode",
                        description = "Read text from signs, books, or documents.",
                        instructions = "• Point camera at text\n• Listen to text being read\n• Perfect for signs and documents"
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Tip: Tap anywhere on the main screen to hear options again!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { 
                            showHelpTutorial = false
                            tts.value?.speak("Help tutorial closed. You're ready to use DRISHTI!", TextToSpeech.QUEUE_FLUSH, null, null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Got it! Let's start!")
                    }
                }
            }
        }
    }
}

@Composable
fun AccessibleFeatureButton(
    icon: ImageVector,
    title: String,
    description: String,
    audioDescription: String,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    var clickCount by remember { mutableStateOf(0) }
    var clickTimer by remember { mutableStateOf<Long?>(null) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                clickCount++
                val currentTime = System.currentTimeMillis()
                
                if (clickCount == 1) {
                    clickTimer = currentTime
                    // Single click - provide audio description
                    // This will be handled by the parent's clickable modifier
                } else if (clickCount == 2) {
                    val timeDiff = currentTime - (clickTimer ?: 0)
                    if (timeDiff < 500) { // Double click within 500ms
                        onDoubleClick()
                        clickCount = 0
                        clickTimer = null
                    } else {
                        clickCount = 1
                        clickTimer = currentTime
                    }
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Double tap to activate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun HelpSection(
    title: String,
    description: String,
    instructions: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = instructions,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
