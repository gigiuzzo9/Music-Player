package com.example.simpleplayer

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
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
                PlayerApp()
            }
        }
    }
}

@Composable
fun PlayerApp() {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }

    // Connessione al PlaybackService tramite MediaController
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

    val c = controller
    if (c == null) {
        // Ancora in connessione col service
        return
    }

    PlayerScreen(controller = c)
}
