package com.involvex.localdreamchat.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.involvex.localdreamchat.data.model.ChatCharacter
import com.involvex.localdreamchat.data.model.ChatMessage
import com.involvex.localdreamchat.data.model.Conversation
import java.io.File

private val CenterVertically: Alignment.Vertical = Alignment.CenterVertically

@Stable
@Composable
fun ChatScreen(
    conversation: Conversation?,
    character: ChatCharacter?,
    model: String,
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    isGeneratingImage: Boolean,
    loadingLLM: Boolean,
    loadingImage: Boolean,
    hasImage: Boolean,
    showImageGenerationButton: Boolean,
    onGenerateImage: () -> Unit,
    onSendMessage: (String) -> Unit,
    onBackPressed: () -> Unit,
    imageGenerationError: String? = null,
    onDismissImageGenerationError: () -> Unit = {},
    onImageGenerationError: (String) -> Unit,
) {
    val textFieldValue = remember { mutableStateOf("") }

    val lazyListState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = CenterVertically,
        ) {
            // Back button
            IconButton(
                onClick = onBackPressed,
                enabled = !isGenerating && !isGeneratingImage,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Title
            Text(
                text = character?.name ?: "Chat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            // Model select
            Text(
                text = "Model: $model",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Messages area
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
        ) {
            items(
                items = messages,
                key = { it.id },
            ) { msg ->
                MessageItem(
                    message = msg,
                    character = character,
                )
            }
        }

        // Loading indicator
        if (isGenerating && !isGeneratingImage) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
            )
        }

        // Image-generation / LLM error banner
        if (imageGenerationError != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = CenterVertically,
                ) {
                    Text(
                        text = imageGenerationError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismissImageGenerationError) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        // Input area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = CenterVertically,
            ) {
                TextField(
                    value = textFieldValue.value,
                    onValueChange = { value ->
                        textFieldValue.value = value
                    },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    enabled = !isGenerating && !isGeneratingImage,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send button
                IconButton(
                    onClick = {
                        if (textFieldValue.value.isNotBlank() && !isGenerating) {
                            onSendMessage(textFieldValue.value)
                            textFieldValue.value = ""
                        }
                    },
                    enabled = !isGenerating && !isGeneratingImage && textFieldValue.value.isNotBlank(),
                ) {
                    if (loadingLLM) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (textFieldValue.value.isNotBlank() && !isGenerating) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            // Image generation button
            if (showImageGenerationButton && character != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val isEnabled = !isGenerating && !isGeneratingImage

                    val isLoading = isGeneratingImage

                    if (isLoading) {
                        Row(
                            verticalAlignment = CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generating image...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        AssistChip(
                            onClick = {
                                if (isEnabled) {
                                    onGenerateImage()
                                }
                            },
                            label = {
                                Row(
                                    verticalAlignment = CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Image,
                                        contentDescription = null,
                                        tint = if (isEnabled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Generate ${character.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            },
                            enabled = isEnabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(
    message: ChatMessage,
    character: ChatCharacter?,
) {
    val isUser = message.role == "user"
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 4.dp,
            ),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (message.imagePath != null) {
            val imageFile = File(context.filesDir, message.imagePath)
            if (imageFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .height(200.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }

        if (message.content.isNotBlank()) {
            Box(
                modifier = Modifier
                    .background(
                        if (isUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        shape = if (isUser) {
                            RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
                        } else {
                            RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                        },
                    )
                    .padding(12.dp),
            ) {
                Column {
                    if (!isUser && character != null) {
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}
