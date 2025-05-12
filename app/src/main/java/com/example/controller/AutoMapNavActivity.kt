package com.example.controller

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.controller.objects.MapOverlayer
import com.example.controller.objects.Ros2MapClient
import com.example.controller.objects.Ros2Nav2Client
import com.example.controller.objects.SharedWebSocketClient
import com.google.android.material.navigation.NavigationView

class AutoMapNavActivity : BaseActivity() {
    //private lateinit var outsideOverlay: View
    private lateinit var goalCounterView: TextView
    private lateinit var goalStatusView: TextView
    private lateinit var mapView: ImageView
    private var isNavOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_auto_map_nav, findViewById(R.id.base_content), true)

        var xCoord: Double? = null
        var yCoord: Double? = null

        goalCounterView = findViewById<TextView>(R.id.statusTitleView)
        goalStatusView = findViewById(R.id.statusView)
        goalCounterView.text = "0/0 Goal(s)"
        goalStatusView.text = "Idle"

        val addBtn = findViewById<ImageButton>(R.id.addImgCoordBtn)
        addBtn.setOnClickListener {
            Toast.makeText(this,"X: $xCoord Y: $yCoord",Toast.LENGTH_SHORT).show()
            xCoord?.let { x ->
                yCoord?.let { y ->
                    Ros2Nav2Client.addGoal(x, y, 0.0)
                    xCoord = null
                    yCoord = null
                    updateView(null)
                }
            }
        }

        val removeBtn = findViewById<ImageButton>(R.id.removeImgCoordBtn)
        removeBtn.setOnClickListener {
            Ros2Nav2Client.removeGoal()
            updateView(null)
        }

        val autoNavBtn = findViewById<ImageButton>(R.id.autoNavBtn)
        autoNavBtn.setOnClickListener {
            if (Ros2Nav2Client.isRunning()) {
                Ros2Nav2Client.cancelNavigation()
                autoNavBtn.setImageResource(R.drawable.baseline_drive_eta_24)
            } else {
                //Change Image
                Ros2Nav2Client.startNavigation()
                autoNavBtn.setImageResource(R.drawable.baseline_cancel_24)
            }
        }

        fun handleMapClick(x: Float, y: Float) {
            val (mapX, mapY) = pixelToMapCoords(x, y) ?: return
            xCoord = mapX
            yCoord = mapY
        }

        //Map
        mapView = findViewById(R.id.imgView)
        mapView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (!Ros2MapClient.mapMetaData.validity)
                    return@setOnTouchListener true
                handleMapClick(event.x, event.y)
            }
            return@setOnTouchListener false
        }

        MapOverlayer.setImageListener={ bitmap->
            runOnUiThread { mapView.setImageBitmap(bitmap) }
        }

        Ros2Nav2Client.onErrorHandler = { msg ->
            runOnUiThread {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                autoNavBtn.setImageResource(R.drawable.baseline_drive_eta_24)
            }
        }

        Ros2Nav2Client.onStatusChange = {
            updateView(it)
        }

        Ros2Nav2Client.onCompletionHandler = {
            runOnUiThread { autoNavBtn.setImageResource(R.drawable.baseline_drive_eta_24) }
        }
    }

    private fun pixelToMapCoords(xPx: Float, yPx: Float): Pair<Double, Double>? {
        val drawable = mapView.drawable ?: return null
        val imageMatrix = mapView.imageMatrix
        val mapMetaData= Ros2MapClient.mapMetaData

        val values = FloatArray(9)
        imageMatrix.getValues(values)
        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]

        // Actual image pixel touched (not view pixel)
        val imgX = ((xPx - transX) / scaleX).toInt()
        val imgY = ((yPx - transY) / scaleY).toInt()

        val mapX = mapMetaData.originX + imgX * mapMetaData.resolution
        val mapY = mapMetaData.originY + (mapMetaData.height - imgY) * mapMetaData.resolution

        return Pair(mapX, mapY)
    }

    private fun updateView(msg: String?) {
        runOnUiThread {
            goalCounterView.text =
                "${Ros2Nav2Client.getGoalCount()}/${Ros2Nav2Client.totalCount()} Goal(s)"
            msg?.let { goalStatusView.text = msg }
        }
    }
}