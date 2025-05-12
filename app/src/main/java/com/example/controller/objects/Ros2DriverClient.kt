package com.example.controller.objects

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

object Ros2DriverClient : Ros2Client(){
    private var pwm_servo1 = 0
    private var pwm_servo2 = -60
    private var isButtonPressed = false
    private val hornMsg = JSONObject()
    var statusListener: ((String) -> Unit)? = null

    private fun subscribeToDriverStatus() {
        try {
            subscribe("/driver_status", "std_msgs/msg/String")
        } catch (e: Exception) {
            Log.e("Ros2DriverClient", "Failed to subscribe to Driver Status topic", e)
        }
        SharedWebSocketClient.driverStatusHandler={
            processData(it)
        }
    }

    private fun processData(data: JSONObject?){
        try {
            val msg = data?.optString("data") ?: return
            statusListener?.invoke(msg)
        }catch (e: Exception){
            Log.e("Ros2DriverClient", "Error processing status message", e)
        }
    }

    fun start(){
        subscribeToDriverStatus()
    }

    private fun setCameraView()
    {
        val message= JSONObject()
        message.put("data",pwm_servo1)
        publish("/servo_s1", "std_msgs/msg/Int32", message)
        message.put("data",pwm_servo2)
        publish("/servo_s2", "std_msgs/msg/Int32", message)
    }

    private fun startHorn()
    {
        if (SharedWebSocketClient.getInstance().isOpen) {
            hornMsg.put("data", 30)
            publish("/beep", "std_msgs/msg/Int32", hornMsg)
        }
    }
    private fun startHornLoop() {
        CoroutineScope(Dispatchers.Main).launch {
            while (isButtonPressed) {
                startHorn()
                delay(200)
            }
            hornMsg.put("data", 0)
            publish("/beep", "std_msgs/msg/Int32", hornMsg)
        }
    }

    fun horn(){
        if (SharedWebSocketClient.getInstance().isOpen) {
            isButtonPressed = !isButtonPressed
            if (isButtonPressed) {
                startHornLoop()
            }
        }
    }

    fun moveCamereUp(){
        if (SharedWebSocketClient.getInstance().isOpen) {
            pwm_servo2 += 10
            if (pwm_servo2 >= 20)
                pwm_servo2 = 20
            setCameraView()
        }
    }

    fun moveCamereDown(){
        if (SharedWebSocketClient.getInstance().isOpen) {
            pwm_servo2 -= 10
            if (pwm_servo2 <= -90)
                pwm_servo2 = -90
        }
        setCameraView()
    }

    fun moveCamereLeft(){
        if (SharedWebSocketClient.getInstance().isOpen) {
            pwm_servo1 -= 10
            if (pwm_servo1 <= -90)
                pwm_servo1 = -90
        }
        setCameraView()
    }

    fun moveCamereRight(){
        if (SharedWebSocketClient.getInstance().isOpen) {
            pwm_servo1 += 10
            if (pwm_servo1 >= 90)
                pwm_servo1 = 90
        }
        setCameraView()
    }

    fun move(linear: Float, angular: Float){
        if(SharedWebSocketClient.getInstance().isOpen) {
            advertise("/custom_cmd_vel","geometry_msgs/msg/Twist")
            val message = JSONObject().apply {
                put("linear", JSONObject().apply {
                    put("x", "%.1f".format(linear * -0.5).toDouble())  // Ensuring numerical format
                    put("y", 0.0)
                    put("z", 0.0)
                })
                put("angular", JSONObject().apply {
                    put("x", 0.0)
                    put("y", 0.0)
                    put("z", "%.1f".format(angular * -0.5).toDouble())  // Ensuring numerical format
                })
            }
            publish("/custom_cmd_vel", "geometry_msgs/msg/Twist", message)
        }
    }
}