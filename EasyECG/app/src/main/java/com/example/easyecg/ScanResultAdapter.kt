package com.example.easyecg

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import kotlinx.android.synthetic.main.row_scan_header.view.*
import kotlinx.android.synthetic.main.row_scan_result.view.*


class ScanResultAdapter(
    private val items: List<ScanResult>,
    private val onClickListener: ((device: ScanResult) -> Unit)
) : Adapter<ViewHolder>() {
    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            1
        } else {
            0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 1){
            val view = LayoutInflater.from(parent.context).inflate(R.layout.row_scan_header,parent,false)
            return ViewholderHeader(view)
        }

        else{
            val view = LayoutInflater.from(parent.context).inflate(R.layout.row_scan_result,parent,false)
            return ViewHolder(view, onClickListener)
        }
    }


    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        if(holder is ViewHolder)
        {
            holder.bind(item)
        }
        else if (holder is ViewholderHeader){
            holder.bind()
        }
    }

    class ViewHolder(
        private val view: View,
        private val onClickListener: ((device: ScanResult) -> Unit)
    ) : RecyclerView.ViewHolder(view) {

        @SuppressLint("MissingPermission")
        fun bind(result: ScanResult) {
            view.device_name.text = result.device.name ?: "Unnamed"
            view.mac_address.text = result.device.address
            view.signal_strength.text = "${result.rssi} dBm"
            view.button.setOnClickListener {
                onClickListener.invoke(result)
            }
        }
    }

    class ViewholderHeader(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(){
            itemView.header.text = "Found Devices"
        }
    }



}


