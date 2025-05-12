package com.example.controller.objects

//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import org.java_websocket.client.WebSocketClient
//import org.java_websocket.handshake.ServerHandshake
//import java.net.URI

import android.util.Log
import kotlinx.coroutines.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI


object SharedWebSocketClient {
    @Volatile
    private var instance: WebSocketClient? = null
    var onConnectionListener: ((Unit)-> Unit)? =null
    var statusListener: ((Boolean) -> Unit)? = null
    var cameraImgHandler: ((JSONObject?)->Unit)?=null
    var laserScanHandler: ((JSONObject?)->Unit)?=null
    var mapHandler: ((JSONObject?)->Unit)?=null
    var nav2StatusHandler: ((JSONObject?)->Unit)?=null
    var poseHandler: ((JSONObject?)->Unit)?=null
    var pathHandler: ((JSONObject?)->Unit)?=null
    var cvImgHandler: ((JSONObject?)->Unit)?=null
    var cvBinaryImgHandler: ((JSONObject?)->Unit)?=null
    var cvStatusHandler: ((JSONObject?)->Unit)?=null
    var driverStatusHandler: ((JSONObject?)->Unit)?=null
    private var connectionJob: Job? = null
    private lateinit var serverUri: URI

    fun getInstance(): WebSocketClient {
        return instance ?: synchronized(this) {
            instance ?: createWebSocketClient().also {
                instance = it
            }
        }
    }

    private fun setServerUri(uri: URI) {
        serverUri=uri
    }

    private fun createWebSocketClient(): WebSocketClient {
        return object : WebSocketClient(serverUri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("WebSocket Connected")
                Ros2DriverClient.start()
                Ros2CameraClient.startStream()
                Ros2CVClient.startStream()
                MapOverlayer.startStream()
                statusListener?.invoke(true)
            }

            override fun onMessage(message: String?) {
                message?.let {
                    try {
                        val json = JSONObject(it)
                        val topic = json.optString("topic", "")
                        val op = json.optString("op", "")
                        val msg = json.optJSONObject("msg")
                        when(topic)
                        {
                            "/robot_pose"-> poseHandler?.invoke(msg)
                            "/map" -> mapHandler?.invoke(msg)
                            "/scan" -> laserScanHandler?.invoke(msg)
//                            "/usb_cam/image_raw/compressed" -> cameraImgHandler?.invoke(msg)
                            "/navigate_to_pose/_action/status"-> nav2StatusHandler?.invoke(msg)
                            "/path"-> pathHandler?.invoke(msg)
//                            "/image" -> cvImgHandler?.invoke(msg)
//                            "/binary_image" -> cvBinaryImgHandler?.invoke(msg)

                            "/camera/image_raw/compressed" -> {
                                msg?.let {
                                    CoroutineScope(Dispatchers.Default).launch {
                                        cameraImgHandler?.invoke(it)
                                    }
                                }
                            }
                            "/image" -> {
                                msg?.let {
                                    CoroutineScope(Dispatchers.Default).launch {
                                        cvImgHandler?.invoke(it)
                                    }
                                }
                            }
                            "/binary_image" -> {
                                msg?.let {
                                    CoroutineScope(Dispatchers.Default).launch {
                                        cvBinaryImgHandler?.invoke(it)
                                    }
                                }
                            }
                            "/status" -> cvStatusHandler?.invoke(msg)
                            "/driver_status" -> driverStatusHandler?.invoke(msg)
                            else -> { }
                        }
                        when(json.optString("op"))
                        {
                            "service_response"-> {
                                val id = json.optString("id")

                                // Match different requests by id or by stored mapping
                                when {
                                    id.startsWith("get_param_") -> {
                                        val paramName = id.removePrefix("get_param_")
                                        handleGetParam(paramName, json)
                                    }
                                    id.startsWith("set_param_") -> {
                                        val paramName = id.removePrefix("set_param_")
                                        handleSetParam(paramName, json)
                                    }
                                    else -> {
                                        Log.w("ROS2", "Unknown service response id: $id")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Ros2WebSocket", "Error retrieving data", e)
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("WebSocket Closed: $reason")
                MapOverlayer.stopStream()
                statusListener?.invoke(false)
            }

            override fun onError(ex: Exception?) {
                println("WebSocket Error: ${ex?.message}")
            }
        }
    }

    fun startConnection(ipAddressStr: String = "") {
        if (ipAddressStr.isNotEmpty()) {
            setServerUri(URI(ipAddressStr))
        }

        connectionJob?.cancel()

        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // Check if instance is null or closed, and recreate only then
                    val socket = instance
                    if (socket == null || !socket.isOpen) {
                        try {
                            socket?.closeBlocking()  // Close if not already
                        } catch (e: Exception) {
                            Log.w("SharedWebSocketClient", "Failed to close: ${e.message}")
                        }

                        instance = createWebSocketClient()
                        withTimeoutOrNull(2000) {
                            instance?.connectBlocking()
                        }
                    }

                    val connected = instance?.isOpen == true
                    statusListener?.invoke(connected)

                    // if (connected) break

                } catch (e: Exception) {
                    Log.e("SharedWebSocketClient", "Error while connecting: ${e.message}")
                    statusListener?.invoke(false)
                }

                delay(1000)
            }
        }
    }

    private fun handleGetParam(paramName: String, json: JSONObject) {
        val values = json.getJSONObject("values")
        val results = values.getJSONArray("results")
        if (results.length() > 0) {
            val result = results.getJSONObject(0)
            val type = result.getInt("type")

            when (paramName) {
//                "status"->
//                "max_speed" -> {
//                    val value = result.optDouble("double_value", 0.0)
//                    handleMaxSpeed(value)
//                }
//                "robot_mode" -> {
//                    val value = result.optString("string_value", "unknown")
//                    handleRobotMode(value)
//                }
                else -> {
                    Log.w("ROS2", "Unknown parameter received: $paramName")
                }
            }
        }
    }

    private fun handleSetParam(paramName: String, json: JSONObject) {
        val success = json.optBoolean("result", false)
        Log.d("ROS2", "Set parameter $paramName result: $success")
    }

}