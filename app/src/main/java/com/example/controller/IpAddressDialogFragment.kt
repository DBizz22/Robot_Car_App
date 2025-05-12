package com.example.controller

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.controller.objects.SharedWebSocketClient
import java.net.URI

class IpAddressDialogFragment(
    private val onEnteredIP: (String)->Unit
) :DialogFragment(){
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        //return super.onCreateDialog(savedInstanceState)
        val input = EditText(requireContext()).apply {
            hint = "192.168.137.112:9090"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Enter IP Address")
            .setView(input)
            .setPositiveButton("Connect") { _, _ ->
                val ip = input.text.toString()
                if (ip.isNotBlank()) {
                    SharedWebSocketClient.startConnection("ws://"+ip)
                    onEnteredIP(ip)
                } else {
                    Toast.makeText(context, "IP address cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}