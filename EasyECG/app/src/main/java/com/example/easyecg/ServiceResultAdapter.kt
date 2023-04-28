package com.example.blekotlinepunchthrough

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattService
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.easyecg.Position
import com.example.easyecg.R
import com.example.easyecg.serviceName
import com.example.easyecg.serviceUUID
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.row_scan_result.view.*
import kotlinx.android.synthetic.main.row_scan_result.view.button
import kotlinx.android.synthetic.main.row_service_result.view.*

val gattDictionary = mapOf(
    "00001800-0000-1000-8000-00805f9b34fb" to "Generic Access",
    "00001801-0000-1000-8000-00805f9b34fb" to "Generic Attribute",
    "0000180a-0000-1000-8000-00805f9b34fb" to "Device Information",
    "0000180d-0000-1000-8000-00805f9b34fb" to "Heart Rate",
    "0000180f-0000-1000-8000-00805f9b34fb" to "Battery Service",
    "6e400001-b5a3-f393-e0a9-e50e24dcca9e" to "ECG Service",
)
class ServiceResultAdapter(
    private val items: List<BluetoothGattService>,
    private val onClickListener: ((service: BluetoothGattService) -> Unit)
) : RecyclerView.Adapter<ServiceResultAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_service_result,parent,false)
        return ViewHolder(view, onClickListener)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position)
    }

    class ViewHolder(
        private val view: View,
        private val onClickListener: ((service: BluetoothGattService) -> Unit)
    ) : RecyclerView.ViewHolder(view) {

        @SuppressLint("MissingPermission")
        fun bind(result: BluetoothGattService, position: Int) {
            val uuid = result.uuid.toString()
            val Index = position
            view.service_address.text = uuid
            view.service_name.text = gattDictionary[uuid]
            //view.textView.text = position.toString()

            view.button.setOnClickListener {
                serviceName = view.service_name.text as String
                serviceUUID = view.service_address.text as String
                Position = Index
                onClickListener.invoke(result)
            }
        }
    }
}