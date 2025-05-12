package com.example.controller

import com.example.controller.objects.Ros2Client
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.graphics.BitmapFactory
import com.example.controller.objects.MapOverlayer
import com.example.controller.objects.Ros2CameraClient
import com.example.controller.objects.Ros2CVClient
import com.example.controller.objects.Ros2DriverClient
import com.example.controller.objects.Ros2LaserClient
import com.example.controller.objects.Ros2Nav2Client
import com.example.controller.objects.Ros2PoseClient
import com.example.controller.objects.SharedWebSocketClient
import com.example.controller.objects.ThumbstickView
import com.github.chrisbanes.photoview.PhotoView
import java.net.URI
import kotlin.math.abs

class MainActivity : BaseActivity() {
    var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_main, findViewById(R.id.base_content), true)
        //setContentView(R.layout.activity_main)


        //enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        //SharedWebSocketClient.startConnection(URI("ws://192.168.137.112:9090"))//("ws://10.42.0.1:9090"))
//        val signalView=findViewById<ImageView>(R.id.signalView)


        val thumbstick1 = findViewById<ThumbstickView>(R.id.thumbstick1)

        // Handle joystick movement
        thumbstick1.joystickListener = joystickListener@{ x, y ->
            if(Ros2Nav2Client.isRunning()) {
                Ros2Nav2Client.cancelNavigation()
                return@joystickListener
            }
            Ros2DriverClient.move(y,x)
        }

        //Camera view control

        val upBtn=findViewById<FloatingActionButton>(R.id.upBtn)
        val downBtn=findViewById<FloatingActionButton>(R.id.downBtn)
        val leftBtn=findViewById<FloatingActionButton>(R.id.leftBtn)
        val rightBtn=findViewById<FloatingActionButton>(R.id.rightBtn)


        upBtn.setOnClickListener {
            Ros2DriverClient.moveCamereUp()
        }
        downBtn.setOnClickListener {
            Ros2DriverClient.moveCamereDown()
        }
        leftBtn.setOnClickListener {
            Ros2DriverClient.moveCamereLeft()
        }
        rightBtn.setOnClickListener {
            Ros2DriverClient.moveCamereRight()
        }

        //View Manager
        var swap=false
        val secondImgView=findViewById<ImageView>(R.id.mapImgView)
        val mainView = findViewById<ImageView>(R.id.imageView)
        //viewManager= ViewManager(imageView, mapView)
        secondImgView.setOnClickListener {
            swap=!swap
        }

        //Video setup
        Ros2CameraClient.imageListener={ bitmap ->
            //runOnUiThread {imageView.setImageBitmap(bitmap)}
            if(!swap)
                runOnUiThread {mainView.setImageBitmap(bitmap)}
            else
                runOnUiThread {secondImgView.setImageBitmap(bitmap)}
        }

        //Map
//        mapClient.mapListener={bitmap ->
//            //runOnUiThread { mapView.setImageBitmap(bitmap) }
//            runOnUiThread { viewManager.getRightView().setImageBitmap(bitmap) }
//        }
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.car)
        Ros2PoseClient.robotBitmap = bitmap
        MapOverlayer.startStream()
        MapOverlayer.setImageListener={ bitmap ->
            if(!swap)
                runOnUiThread {secondImgView.setImageBitmap(bitmap)}
            else
                runOnUiThread {mainView.setImageBitmap(bitmap)}
        }

        //Horn button
        val hornBtn = findViewById<Button>(R.id.hornBtn)

        hornBtn.setOnClickListener {
            Ros2DriverClient.horn()
        }

        Ros2DriverClient.statusListener={ msg->
            runOnUiThread {showToast(this,msg)}
        }
    }

    fun showToast(context: Context, message: String) {
        if (toast == null) {
            toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        } else {
            toast?.setText(message)
        }
        toast?.show()
    }

    override fun onPause() {
        super.onPause()
    }
    override fun onDestroy() {
        //webserver?.close()
        //SharedWebSocketClient.statusListener=null
        SharedWebSocketClient.getInstance().close()
        super.onDestroy()
    }
}