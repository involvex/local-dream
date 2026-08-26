package com.involvex.localdreamchat

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.involvex.localdreamchat.data.MigrationState
import com.involvex.localdreamchat.data.db.AppDatabase
import com.involvex.localdreamchat.data.repository.ChatRepository
import com.involvex.localdreamchat.navigation.Screen
import com.involvex.localdreamchat.ui.screens.CharacterListScreen
import com.involvex.localdreamchat.ui.screens.ChatScreen
import com.involvex.localdreamchat.ui.screens.ChatViewModel
import com.involvex.localdreamchat.ui.screens.HistoryScreen
import com.involvex.localdreamchat.ui.screens.MigrationScreen
import com.involvex.localdreamchat.ui.screens.ModelListScreen
import com.involvex.localdreamchat.ui.screens.ModelRunScreen
import com.involvex.localdreamchat.ui.screens.RemoteScreen
import com.involvex.localdreamchat.ui.screens.UpscaleScreen
import com.involvex.localdreamchat.ui.theme.LocalDreamTheme
import com.involvex.localdreamchat.ui.theme.LocalThemeController
import com.involvex.localdreamchat.ui.theme.rememberThemeController
import com.involvex.localdreamchat.ui.theme.sharedAxisXEnter
import com.involvex.localdreamchat.ui.theme.sharedAxisXExit
import com.involvex.localdreamchat.ui.theme.sharedAxisXPopEnter
import com.involvex.localdreamchat.ui.theme.sharedAxisXPopExit
import com.involvex.localdreamchat.ui.theme.sharedAxisXPredictivePopEnter
import com.involvex.localdreamchat.ui.theme.sharedAxisXPredictivePopExit

class MainActivity : ComponentActivity() {
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(
                this,
                getString(R.string.permission_storage_required),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(
                this,
                getString(R.string.permission_notification_required),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun checkStoragePermission() {
        // < Android 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // ok
                }

                shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                    Toast.makeText(
                        this,
                        getString(R.string.permission_storage_required),
                        Toast.LENGTH_LONG,
                    ).show()
                    requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }

                else -> {
                    requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        // > Android 13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // ok
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(
                        this,
                        getString(R.string.permission_notification_required),
                        Toast.LENGTH_LONG,
                    ).show()
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                else -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkStoragePermission()
        checkNotificationPermission()

        val app = application as LocalDreamApplication

        setContent {
            val themeController = rememberThemeController()
            CompositionLocalProvider(LocalThemeController provides themeController) {
                LocalDreamTheme(themeController.state) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        val migrationState by app.migrationState.collectAsState()

                        when (migrationState) {
                            is MigrationState.Done,
                            is MigrationState.NotNeeded,
                            -> AppContent()

                            is MigrationState.Idle,
                            is MigrationState.InProgress,
                            is MigrationState.Failed,
                            -> MigrationScreen(
                                state = migrationState,
                                onRetry = { app.retryMigration() },
                                onSkip = { app.skipMigration() },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun chatViewModelFactory(
    application: Application,
    characterId: String,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(application, characterId) as T
}

@Composable
private fun AppContent() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.ModelList.route,
        enterTransition = { sharedAxisXEnter() },
        exitTransition = { sharedAxisXExit() },
        popEnterTransition = { sharedAxisXPopEnter() },
        popExitTransition = { sharedAxisXPopExit() },
        predictivePopEnterTransition = { _ -> sharedAxisXPredictivePopEnter() },
        predictivePopExitTransition = { _ -> sharedAxisXPredictivePopExit() },
    ) {
        composable(Screen.ModelList.route) {
            ModelListScreen(navController)
        }
        composable(
            route = Screen.ModelRun.route,
            arguments = listOf(
                navArgument("modelId") {
                    type = NavType.StringType
                },
                navArgument("remote") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { backStackEntry ->
            val modelId = backStackEntry.arguments?.getString("modelId") ?: ""
            val isRemote = backStackEntry.arguments?.getBoolean("remote") ?: false

            ModelRunScreen(
                modelId = modelId,
                isRemote = isRemote,
                navController = navController,
            )
        }
        composable(Screen.Upscale.route) {
            UpscaleScreen(navController)
        }
        composable(Screen.History.route) {
            HistoryScreen(navController)
        }
        composable(Screen.RemoteLink.route) {
            RemoteScreen(navController)
        }
        composable(Screen.CharacterList.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val repository = remember { ChatRepository.get(AppDatabase.get(context)) }
            CharacterListScreen(
                navController = navController,
                repository = repository,
            )
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("characterId") {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId") ?: ""
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as ComponentActivity
            val application = context.applicationContext as Application

            val viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                viewModelStoreOwner = activity,
                factory = chatViewModelFactory(application, characterId),
            )

            val character by viewModel.character.collectAsState()
            val conversation by viewModel.conversation.collectAsState()
            val messages by viewModel.messages.collectAsState()
            val isGenerating by viewModel.isGenerating.collectAsState()
            val isGeneratingImage by viewModel.isGeneratingImage.collectAsState()
            val loadingLLM by viewModel.loadingLLM.collectAsState()
            val loadingImage by viewModel.loadingImage.collectAsState()
            val hasImage by viewModel.hasImage.collectAsState()
            val showImageGenerationButton by viewModel.showImageGenerationButton.collectAsState()
            val imageGenerationError by viewModel.imageGenerationError.collectAsState()

            val modelName = "SD1.5"

            LaunchedEffect(characterId) {
                viewModel.loadCharacter(characterId)
            }

            ChatScreen(
                conversation = conversation,
                character = character,
                model = modelName,
                messages = messages,
                isGenerating = isGenerating,
                isGeneratingImage = isGeneratingImage,
                loadingLLM = loadingLLM,
                loadingImage = loadingImage,
                hasImage = hasImage,
                showImageGenerationButton = showImageGenerationButton && character != null,
                onGenerateImage = { viewModel.generateCharacterImage() },
                onSendMessage = { message ->
                    viewModel.sendMessage(message)
                },
                onBackPressed = { navController.popBackStack() },
                imageGenerationError = imageGenerationError,
                onDismissImageGenerationError = { viewModel.onImageGenerationError("") },
                onImageGenerationError = { error -> viewModel.onImageGenerationError(error) },
            )
        }
    }
}
