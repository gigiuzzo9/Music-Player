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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Leggi l'URI ricevuto da "Apri con"
        val incomingUri: Uri? = intent?.data

        setContent {
            SimplePlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlayerApp(incomingUri = incomingUri)
                }
            }
        }
    }
}

@Composable
fun PlayerApp(incomingUri: Uri?) {
    val context = LocalContext.current
    var service by remember { mutableStateOf<PlaybackService?>(null) }

    DisposableEffect(Unit) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val localBinder = binder as PlaybackService.LocalBinder
                service = localBinder.getService()

                // Se è arrivato un URI da "Apri con", riproducilo
                if (incomingUri != null) {
                    val title = incomingUri.lastPathSegment ?: "Brano"
                    service?.playQueue(
                        listOf(incomingUri.toString() to title),
                        0
                    )
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
