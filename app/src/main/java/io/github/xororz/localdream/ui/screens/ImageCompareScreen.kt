package io.github.xororz.localdream.ui.screens

import android.net.Uri
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.xororz.localdream.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCompareScreen(
    leftPath: String,
    rightPath: String,
    navController: NavController
) {
    var leftScale by remember { mutableStateOf(1f) }
    var leftOffsetX by remember { mutableStateOf(0f) }
    var leftOffsetY by remember { mutableStateOf(0f) }
    var rightScale by remember { mutableStateOf(1f) }
    var rightOffsetX by remember { mutableStateOf(0f) }
    var rightOffsetY by remember { mutableStateOf(0f) }
    var isSwapped by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun resolveImage(path: String): Any {
        return if (path.startsWith("content://") || path.startsWith("file://")) {
            Uri.parse(path)
        } else {
            File(path)
        }
    }

    val leftImage = resolveImage(if (isSwapped) rightPath else leftPath)
    val rightImage = resolveImage(if (isSwapped) leftPath else rightPath)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.image_compare_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSwapped = !isSwapped
                        // Reset transforms on swap
                        leftScale = 1f; leftOffsetX = 0f; leftOffsetY = 0f
                        rightScale = 1f; rightOffsetX = 0f; rightOffsetY = 0f
                    }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.swap_images))
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Left image
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            leftScale = (leftScale * zoom).coerceIn(0.5f, 5f)
                            leftOffsetX += pan.x
                            leftOffsetY += pan.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(leftImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.left_image),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = leftScale,
                            scaleY = leftScale,
                            translationX = leftOffsetX,
                            translationY = leftOffsetY
                        ),
                    contentScale = ContentScale.Fit
                )
            }

            // Divider
            Spacer(modifier = Modifier.height(4.dp))

            // Right image
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            rightScale = (rightScale * zoom).coerceIn(0.5f, 5f)
                            rightOffsetX += pan.x
                            rightOffsetY += pan.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(rightImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.right_image),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = rightScale,
                            scaleY = rightScale,
                            translationX = rightOffsetX,
                            translationY = rightOffsetY
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
