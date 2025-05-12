package com.example.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.controller.objects.MapOverlayer
import com.example.controller.objects.Ros2CVClient
import com.example.controller.objects.Ros2CameraClient
import com.example.controller.objects.SharedWebSocketClient
import com.google.android.material.navigation.NavigationView
import java.net.URI

open class BaseActivity : AppCompatActivity() {
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationPanel: NavigationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        super.setContentView(R.layout.activity_base)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, _ ->
            v.setPadding(0, 0, 0, 0)
            WindowInsetsCompat.CONSUMED
        }


        drawerLayout = findViewById(R.id.main)
        navigationPanel = findViewById(R.id.navView)

        navigationPanel.setNavigationItemSelectedListener { menuItem ->
            val currentActivity = this::class.java

            when (menuItem.itemId) {
                R.id.nav_manual -> {
                    if (currentActivity != MainActivity::class.java) {
                        startActivity(Intent(this, MainActivity::class.java))
                        //finish() // Optional: to remove the current activity from the back stack
                    }
                }

                R.id.nav_auto -> {
                    if (currentActivity != AutoNavActivity::class.java) {
                        startActivity(Intent(this, AutoNavActivity::class.java))
                        //finish()
                    }
                }

                R.id.nav_auto_map -> {
                    if (currentActivity != AutoMapNavActivity::class.java) {
                        startActivity(Intent(this, AutoMapNavActivity::class.java))
                        //finish()
                    }
                }

                R.id.nav_img->{
                    if (currentActivity != ImageActivity::class.java){
                        startActivity(Intent(this, ImageActivity::class.java))
                        //finish()
                    }
                }

                R.id.nav_IP -> {
                    val dialog = IpAddressDialogFragment { ip ->
                        Log.d("BaseActivity", "IP Entered: $ip")
                    }
                    dialog.show(supportFragmentManager, "IpDialog")
                }
            }

            drawerLayout.closeDrawers()
            true
        }

        val settingsBtn = findViewById<ImageView>(R.id.settingsView)
        val drawerLayout = findViewById<DrawerLayout>(R.id.main)

        settingsBtn.setOnClickListener {
            if (drawerLayout.isDrawerOpen(navigationPanel)) {
                drawerLayout.closeDrawer(navigationPanel)
            } else {
                drawerLayout.openDrawer(navigationPanel)
            }
        }
        val signalView=findViewById<ImageView>(R.id.signalView)
        //SharedWebSocketClient.startConnection()
        var currentStatus = SharedWebSocketClient.getInstance().isOpen
        if(currentStatus)
            signalView.setImageResource(R.drawable.baseline_wifi_24)
        else
            signalView.setImageResource(R.drawable.baseline_wifi_off_24)
        SharedWebSocketClient.statusListener = { status ->
            runOnUiThread {

                if (currentStatus!=status) {
                    val msg = if (status) {
                        signalView.setImageResource(R.drawable.baseline_wifi_24)
                        "Connected"
                    } else {
                        signalView.setImageResource(R.drawable.baseline_wifi_off_24)
                        "Disconnected"
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
                currentStatus = status
            }
        }
    }
}
