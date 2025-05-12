package com.example.controller

import android.app.Application
import com.example.controller.objects.SharedWebSocketClient
import java.net.URI

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        SharedWebSocketClient.startConnection("ws://192.168.137.112:9090")
    }
}