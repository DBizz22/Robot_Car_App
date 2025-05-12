package com.example.controller

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.animation.addListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.controller.objects.Ros2CVClient
import com.example.controller.objects.SharedWebSocketClient
import com.google.android.material.navigation.NavigationView

class ImageActivity : BaseActivity() {
    private lateinit var imgView: ImageView
    private lateinit var overlayedImgView: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_image, findViewById(R.id.base_content), true)

        var xCoord: Int? = null
        var yCoord: Int? = null

        Ros2CVClient.startStream()
        val addBtn = findViewById<ImageButton>(R.id.addImgCoordBtn)
        addBtn.setOnClickListener {
            Toast.makeText(this, "X: $xCoord Y: $yCoord", Toast.LENGTH_SHORT).show()
            xCoord?.let { x ->
                yCoord?.let { y ->
                    Ros2CVClient.setLineFollowerPixel(x, y)
                    Ros2CVClient.setLineFollowerEvent(2)
                    xCoord = null
                    yCoord = null
                }
            }
        }

        val removeBtn = findViewById<ImageButton>(R.id.removeImgCoordBtn)
        removeBtn.setOnClickListener {
            Ros2CVClient.setLineFollowerEvent(1)
            xCoord = null
            yCoord = null
        }

        val autoNavBtn = findViewById<ImageButton>(R.id.autoNavBtn)
        autoNavBtn.setOnClickListener {
            //autoNavBtn.setImageResource(R.drawable.baseline_drive_eta_24)
            Ros2CVClient.setLineFollowerEvent(3)
        }

        fun handleImgClick(x: Float, y: Float): Boolean {
            val (mapX, mapY) = getBitmapCoordsFromTouch(x, y) ?: return false
            xCoord = mapX
            yCoord = mapY
            return true
        }

        //Image
        var swap =false
        imgView = findViewById(R.id.imgView)
        overlayedImgView = findViewById(R.id.overlayedImgView)
        //val viewManager= ViewManager(imgView, overlayedImgView)
        overlayedImgView.setOnClickListener {
            swap=!swap
        }
        imgView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                return@setOnTouchListener handleImgClick(event.x, event.y)
            }
            return@setOnTouchListener false
        }
        Ros2CVClient.imageListener={ bitmap->
            if(!swap)
                runOnUiThread { imgView.setImageBitmap(bitmap) }
            else
                runOnUiThread { overlayedImgView.setImageBitmap(bitmap) }
        }
        Ros2CVClient.binaryImageListener={ bitmap->
            if(!swap)
                runOnUiThread { overlayedImgView.setImageBitmap(bitmap) }
            else
                runOnUiThread { imgView.setImageBitmap(bitmap) }
        }
        var statusView=findViewById<TextView>(R.id.statusView)
        Ros2CVClient.statusListener={status->
            runOnUiThread {statusView.text=status}
        }

        //Tracker Mode
        val statusBox=findViewById<CardView>(R.id.statusBox)
        Ros2CVClient.setHandTrackerMode()
        var modeSwitch=findViewById<Switch>(R.id.modeSwitch)
        modeSwitch.setOnCheckedChangeListener { _,isChecked->
            modeSwitch.text = if (isChecked){
                Ros2CVClient.setLineFollowerMode()
                "Line"
            } else {
                Ros2CVClient.setHandTrackerMode()
                "Hand"
            }

            val visibility = if (isChecked) View.VISIBLE else View.INVISIBLE

//            statusBox.visibility = visibility
            addBtn.visibility = visibility
            removeBtn.visibility = visibility
            autoNavBtn.visibility = visibility
            overlayedImgView.visibility = visibility

            if(!isChecked) swap=false
        }
    }

    override fun onPause() {
        super.onPause()
        Ros2CVClient.closeTrackerMode()
    }

    private fun getBitmapCoordsFromTouch(xPx: Float, yPx: Float): Pair<Int, Int>? {
        val drawable = imgView.drawable ?: return null

        val values = FloatArray(9)
        imgView.imageMatrix.getValues(values)

        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]

        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return null

        // Undo translation and scaling to get bitmap coordinate
        val imgX = ((xPx - transX) / scaleX).toInt()
        val imgY = ((yPx - transY) / scaleY).toInt()

        // Clamp to bitmap bounds
        val clampedX = imgX.coerceIn(0, bitmap.width - 1)
        val clampedY = imgY.coerceIn(0, bitmap.height - 1)

        return Pair(clampedX, clampedY)
    }
}