package com.example.simpleplayer

import android.content.ComponentName
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.simpleplayer.ui.PlayerScreen
import com.example.simpleplayer.ui.theme.SimplePlayerTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SimplePlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlayerApp()
                }
            }
        }
    }
}

@Composable
fun PlayerApp() {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }

    // Chiedi il permesso MANAGE_EXTERNAL_STORAGE all'avvio
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            }
        }
    }

    // Connessione al PlaybackService tramite MediaController (in background)
    DisposableEffect(Unit) {
        val token = SessionToken(
            context,
            ComponentName(context, com.example.simpleplayer.playback.PlaybackService::class.java)
        )
        val future: ListenableFuture<MediaController> =
            MediaController.Builder(context, token).buildAsync()

        future.addListener(
            { controller = future.get() },
            MoreExecutors.directExecutor()
        )

        onDispose {
            controller?.release()
            controller = null
            MediaController.releaseFuture(future)
        }
    }

    // La UI si mostra SEMPRE, anche se il controller non è pronto
    PlayerScreen(controller = controller)
}
