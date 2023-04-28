package com.example.easyecg

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_device_status.*
import org.w3c.dom.Text


class deviceStatusFragment : Fragment() {

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_device_status, container, false)
        val NextButton: Button = view.findViewById(R.id.prevButton)
        val Button5: Button = view.findViewById(R.id.button5)
        val activityName = activity?.javaClass?.simpleName
        val intent = Intent(context, MainActivity::class.java)

        if(activityName == "MainActivity2"){
            val address_Text: TextView = view.findViewById(R.id.address_text)
            val deviceName_Text: TextView = view.findViewById(R.id.deviceName_text)
            val status_Text: TextView = view.findViewById(R.id.status_text)
            val RSSI_Text: TextView = view.findViewById(R.id.RSSI_text)
            val ServiceName: TextView = view.findViewById(R.id.textView2)
            val ServiceUUID: TextView = view.findViewById(R.id.textView7)
            address_Text.text = bluetoothGatt.device.address
            deviceName_Text.text = bluetoothGatt.device.name
            status_Text.text = "connected"
            RSSI_Text.text = RSSI.toString()
            ServiceName.text = serviceName
            ServiceUUID.text = serviceUUID
        }

        Button5.setOnClickListener(){
            bluetoothGatt.readRemoteRssi()
            RSSI_text.text = RSSI.toString()
        }


        NextButton.setOnClickListener{
            bluetoothGatt.disconnect()
            bluetoothGatt.close()
            serviceResults.clear()
            //(activity as MainActivity).stopBleScan()
            //it.visibility = View.GONE
            if(activityName == "MainActivity"){
                val ScanButton: Button = requireActivity().findViewById(R.id.scanButton)
                val ServiceResultView: RecyclerView = requireActivity().findViewById(R.id.serviceResultView)
                val UserMode: Button = requireActivity().findViewById(R.id.userMode)
                val DeviceStatus: TextView = requireActivity().findViewById(R.id.deviceStatus)
                val Nav_container: FrameLayout = requireActivity().findViewById(R.id.nav_container)
                ScanButton.visibility = View.VISIBLE
                ServiceResultView.visibility = View.GONE
                UserMode.visibility = View.GONE
                DeviceStatus.text = "Device Not Connected"
                Nav_container.visibility = View.GONE
            }
            else{
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                (activity as MainActivity2).handler.removeMessages(0)
            }
        }
        return view
    }
}