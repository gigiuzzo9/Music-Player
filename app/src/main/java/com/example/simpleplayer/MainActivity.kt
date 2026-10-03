package com.example.simpleplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.simpleplayer.playback.PlaybackService
import com.example.simpleplayer.ui.PlayerScreen
import com.example.simpleplayer.ui.theme.SimplePlayerTheme

class MainActivity : ComponentActivity() {

    // URI del file audio passato dall'esterno (null se apriamo normalmente)
    private var pendingAudioUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Leggi l'URI all'avvio
        pendingAudioUri = extractAudioUri(intent)

        setContent {
            SimplePlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlayerApp(pendingAudioUri)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Se l'app è già aperta e arriva un nuovo file audio
        val uri = extractAudioUri(intent)
        if (uri != null) {
            pendingAudioUri = uri
            // Notifica il composable che c'è un nuovo URI
            // (nel nostro caso lo gestiamo tramite il service nel composable)
        }
    }

    private fun extractAudioUri(intent: Intent?): Uri? {
        if (intent?.action == Intent.ACTION_VIEW) {
            return intent.data
        }
        return null
    }
}

@Composable
fun PlayerApp(initialUri: Uri?) {
    val context = LocalContext.current
    var service by remember { mutableStateOf<PlaybackService?>(null) }

    DisposableEffect(Unit) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val localBinder = binder as PlaybackService.LocalBinder
                val svc = localBinder.getService()
                service = svc

                // Se c'è un URI pendente, riproducilo
                if (initialUri != null) {
                    svc.playFromUri(context, initialUri)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }

        val intent = Intent(context, PlaybackService::class.java)
        context.startService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        onDispose {
            context.unbindService(connection)
        }
    }

    val s = service ?: return
    PlayerScreen(service = s)
}
