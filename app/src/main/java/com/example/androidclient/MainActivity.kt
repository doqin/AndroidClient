package com.example.androidclient

import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.example.androidclient.ui.theme.AndroidClientTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket

class MainActivity : ComponentActivity() {
    private val broadcastPort = 5000
    private val tcpPort = 8080
    private val timeout = 5000 // 5 seconds
    private var udpJob: Job? = null
    private var tcpJob: Job? = null
    private var serverIp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startListeningForBroadcast()
        enableEdgeToEdge()
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        setContent {
            AndroidClientTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Trackpad")
                        }
                    }
                    TouchEvents()
                }
            }
        }
    }

    private fun sendMessage(serverIp: String) {
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(serverIp, tcpPort), timeout)
            val output = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
            output.println("Hello from Android client!")
        } catch (e: Exception) {
            e.printStackTrace()
            // Popup error message to user
        }
    }

    private fun startListeningForBroadcast() {
        udpJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = DatagramSocket(broadcastPort)
                socket.broadcast = true
                val buffer = ByteArray(1024)

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)

                    val receivedMessage = String(packet.data, 0, packet.length)
                    serverIp = packet.address.hostAddress
                    Log.d("UDP", "Server found at: $serverIp")
                    Toast.makeText(
                        this@MainActivity,
                        "Server found!",
                        Toast.LENGTH_SHORT)
                        .show()
                    if (receivedMessage.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            serverIp?.let { connectToServer(it) }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UDP", "Error: ${e.message}")
            }
        }
    }

    private fun connectToServer(serverIp: String) {
        val context = this
        tcpJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(serverIp, tcpPort), timeout)
                val output = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
                // val input = BufferedReader(InputStreamReader(socket.getInputStream()))

                Log.d("TCP", "Connected to server at $serverIp")
                output.println("Hello from Android client!")
                Toast.makeText(context, "Message sent to server", Toast.LENGTH_SHORT)
                    .show()

                // val response = input.readLine()
                // Log.d("TCP", "Server response: $response")
                // Toast.makeText(context, "Message from server: $response", Toast.LENGTH_LONG).show()

                while(socket.isConnected) {
                    // TODO: Handle incoming messages
                }

                Toast.makeText(context, "Connection lost", Toast.LENGTH_LONG).show()
                Log.e("TCP", "Connection lost, restarting UDP discovery...")

            } catch (e: Exception) {
                e.printStackTrace()
                // Popup error message to user
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.d("TCP", "Connection error: ${e.message}, restarting UDP discovery...")
            }

            withContext(Dispatchers.Main) {
                startListeningForBroadcast()
            }
        }
    }
    @Composable
    private fun TouchEvents() {
        var touchPosition by remember {
            mutableStateOf(Offset.Zero) }
        var previousTouchPosition by remember {
            mutableStateOf(Offset.Zero)
        }
        var isTouching by remember {
            mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isTouching = true
                            touchPosition = offset
                            previousTouchPosition = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val currentTouchPosition = change.position
                            val dx = (currentTouchPosition.x - previousTouchPosition.x).toInt()
                            val dy = (currentTouchPosition.y - previousTouchPosition.y).toInt()
                            sendMouseEvent(
                                serverIp ?: "",
                                "MOVE",
                                dx,
                                dy)
                            previousTouchPosition = currentTouchPosition
                        },
                        onDragEnd = {
                            isTouching = false
                        }
                    )
                }
        )
    }

    private fun sendMouseEvent(serverIp: String, eventType: String, x: Int = 0, y: Int = 0) {
        if (serverIp.isEmpty()) return
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(serverIp, tcpPort), timeout)
            val output = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
            val message = when (eventType) {
                "MOVE" -> "MOVE $x $y"
                "LEFT_DOWN" -> "LEFT_DOWN"
                "LEFT_UP" -> "LEFT_UP"
                else -> ""
            }
            output.println(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        udpJob?.cancel()
        tcpJob?.cancel()
    }
}