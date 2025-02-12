package com.example.androidclient

import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.androidclient.ui.theme.AndroidClientTheme
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket

class MainActivity : ComponentActivity() {
    private val serverIp = "127.0.0.1"
    private val serverPort = "8080"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    )
                    {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Say hello to the server!")
                            IconButton(onClick = { connectToServer() }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun connectToServer() {
        try {
            val serverAddr = InetAddress.getByName(serverIp)
            val socket = Socket(serverAddr, serverPort.toInt())
            val out = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
            out.println("Hello from Android client!")
            socket.close()
            // Popup message to user
            Toast.makeText(this, "Message sent to server", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            // Popup error message to user
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            Log.d("MainActivity", "Error: ${e.message}")
        }
    }
}