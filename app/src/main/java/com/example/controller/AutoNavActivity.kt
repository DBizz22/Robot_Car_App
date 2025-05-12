package com.example.controller

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
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
import com.example.controller.objects.Ros2Nav2Client
import com.example.controller.objects.SharedWebSocketClient
import com.google.android.material.navigation.NavigationView
import org.java_websocket.client.WebSocketClient

class AutoNavActivity : BaseActivity() {
    var webserver: WebSocketClient? = null
    private lateinit var outsideOverlay: View
    private lateinit var goalCounterView: TextView
    private lateinit var goalStatusView: TextView
    private var isNavOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_auto_nav, findViewById(R.id.base_content), true)

        //Coords input
        val xCoordInput=findViewById<EditText>(R.id.xCoordInput)
        val yCoordInput=findViewById<EditText>(R.id.yCoordInput)
        val thetaCoordInput=findViewById<EditText>(R.id.thetaCoordInput)
        val addCoordBtn=findViewById<ImageButton>(R.id.addCoordBtn)
        val removeCoordBtn=findViewById<ImageButton>(R.id.removeBtn)
        val autoNavBtn=findViewById<ImageButton>(R.id.autoDriveBtn)

        goalCounterView=findViewById<TextView>(R.id.statusTitleView)
        goalStatusView=findViewById(R.id.statusView)

        addCoordBtn.setOnClickListener {
            val xText = xCoordInput.text.toString()
            val yText = yCoordInput.text.toString()
            val thetaText = thetaCoordInput.text.toString()

            if (xText.isBlank() || yText.isBlank() || thetaText.isBlank()) {
                Toast.makeText(this, "Fill Coord", Toast.LENGTH_SHORT).show()
            } else {
                val xCoord = xText.toDouble()
                val yCoord = yText.toDouble()
                val thetaCoord = thetaText.toDouble()
                Ros2Nav2Client.addGoal(xCoord,yCoord,thetaCoord)
            }
            updateView(null)
        }

        removeCoordBtn.setOnClickListener {
            Ros2Nav2Client.removeGoal()
            updateView(null)
        }

        autoNavBtn.setOnClickListener {
            if (Ros2Nav2Client.isRunning())
            {
                autoNavBtn.setImageResource(R.drawable.baseline_drive_eta_24)
                Ros2Nav2Client.cancelNavigation()
            }
            else {
                //Change Image
                autoNavBtn.setImageResource(R.drawable.baseline_cancel_24)
                Ros2Nav2Client.startNavigation()
            }
        }

        Ros2Nav2Client.onErrorHandler={ msg->
            runOnUiThread {
                Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
                autoNavBtn.setImageResource(R.drawable.baseline_drive_eta_24)}
        }

        goalCounterView.text="0/0 Goal(s)"
        goalStatusView.text="Idle"
        Ros2Nav2Client.onStatusChange={
           updateView(it)
        }

        Ros2Nav2Client.onCompletionHandler={
            runOnUiThread{autoNavBtn.setImageResource(R.drawable.baseline_drive_eta_24)}
        }
    }

    private fun updateView(msg: String?) {
        runOnUiThread {
            goalCounterView.text = "${Ros2Nav2Client.getGoalCount()}/${Ros2Nav2Client.totalCount()} Goal(s)"
            msg?.let { goalStatusView.text=msg }  }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}