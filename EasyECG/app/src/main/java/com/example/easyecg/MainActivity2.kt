package com.example.easyecg

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main2.*
import kotlinx.android.synthetic.main.fragment_device_status.*
import java.util.*
import kotlin.properties.Delegates

private var characterFirstRead: Int = 0

private var dataAvailable = false
private var dataAvailable2 = false
private var dataAvailable3 = false
private var nextUpdateArrived = false

private val data = FloatArray(120)
private val data2 = FloatArray(2)
private val data3 = FloatArray(2)

private var counter2 = 0
private var isCChecked = false

var num by Delegates.notNull<Int>()
var receiveBuffer = arrayOf<Int>()

var receiveBufferMut = receiveBuffer.toMutableList()
var receiveBufferMut2 = receiveBuffer.toMutableList()
var receiveBufferMut3 = receiveBuffer.toMutableList()


class MainActivity2 : AppCompatActivity() {
    private var initialized = false
    val xvalue = ArrayList<String>()
    var values = ArrayList<Entry>()
    val xvalue2 = ArrayList<String>()
    var values2 = ArrayList<Entry>()
    val xvalue3 = ArrayList<String>()
    var values3 = ArrayList<Entry>()

    private val updatePeriod = 8

    private var index = 0
    private var index2 = 0
    private var readingcounter2 = 0
    private var index3 = 0
    private var readingcounter3 = -1
    private var previousEntry = 0f
    private var firstBatteryLvl = 0f

    val handler: Handler = Handler()

    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            val action = intent.action
            if (MainActivity.ACTION_GATT_DISCONNECTED.equals(action)) {
                status_text.text = "disconnected"
            }
            if (MainActivity.ACTION_DATA_AVAILABLE.equals(action)) {
                val value = intent.getByteArrayExtra(MainActivity.EXTRA_DATA)
                if(serviceUUID=="0000180f-0000-1000-8000-00805f9b34fb" && value != null){
                    nextUpdateArrived = true
                    val buffer: ULong = 0u
                    var batterylevel = (buffer and 0xFFu) shl 8 or value[0].toULong() and 0xFFu
                    var batterylevelInt = batterylevel.toInt()
                    if (batterylevelInt < 0){
                        batterylevelInt += 128
                    }
                    batterylevelInt += 180
                    textView4.text = batterylevelInt.toString()
                    receiveBufferMut3.add(receiveBufferMut3.size, batterylevelInt)
                    Log.i("heartrate", "type is ${batterylevelInt::class.simpleName + 128} heart-rate is ${batterylevelInt}")

                    dataAvailable3 = true

                    var counter: Int = 0
                    receiveBufferMut3.forEach(){heartRateReading->
                        data3[counter] = heartRateReading.toFloat()
                        counter++
                    }
                    receiveBufferMut3 = receiveBuffer.toMutableList()
                    nextUpdateArrived = false
                }

                else if(serviceUUID=="0000180d-0000-1000-8000-00805f9b34fb" && value != null) {
                    value.forEach { reading->
                        Log.i("heartrate reading", "type is ${reading::class.simpleName} heart-rate is $reading")
                   }
                    val buffer: ULong = 0u
                    var heartRate = (buffer and 0xFFu) shl 8 or value[1].toULong() and 0xFFu
                    var heartRateInt = heartRate.toInt()
                    if (heartRateInt < 0){
                        heartRateInt += 128
                    }
                    textView4.text = heartRateInt.toString()
                    receiveBufferMut2.add(receiveBufferMut2.size, heartRateInt)
                    Log.i("heartrate", "type is ${heartRate::class.simpleName + 128} heart-rate is ${heartRate}")

                    dataAvailable2 = true

                    var counter: Int = 0
                    receiveBufferMut2.forEach(){heartRateReading->
                        data2[counter] = heartRateReading.toFloat()
                        counter++
                    }
                    receiveBufferMut2 = receiveBuffer.toMutableList()
                }

                if (value != null) {
                    if (value.size > 60) {
                        val buffer: ULong = 0u
                        var x = ((buffer and 0xFFu) shl 8 or value[120].toULong() and 0xFFu).toInt()
                        var y = ((buffer and 0xFFu) shl 8 or value[121].toULong() and 0xFFu).toInt()
                        var z = ((buffer and 0xFFu) shl 8 or value[122].toULong() and 0xFFu).toInt()

                        if (x < 0){
                            x += 128
                        }
                        if (y < 0){
                            y += 128
                        }
                        if (z < 0){
                            z += 128
                        }
                        textView5.text = x.toString()
                        textView6.text = y.toString()
                        textView8.text = z.toString()
                        for (i in 0 until 40) {

                            val byte1 = (value[i*3].toULong() and 0xFFu) shl 8
                            val byte2 = value[i*3 + 1].toULong() and 0xFFu
                            val byte = byte1 or byte2
                            val byte3 = byte.toInt()
                            if (byte3 > 32767) {
                                val byte4 = (65535 - byte3).inv()
                                num = byte4
                                receiveBufferMut.add(receiveBufferMut.size, byte4)
                                messageHandler()

                            } else {
                                num = byte3
                                receiveBufferMut.add(receiveBufferMut.size, byte3)
                                messageHandler()
                            }

                        }

                        for (i in 0 until receiveBufferMut.size) {
                            var value: Float
                            try {
                                value = receiveBufferMut.get(i).toFloat()
                            } catch (e: Exception) {
                                value = 0f
                                e.printStackTrace()
                                println("Parsing error.")
                            }
                            if (value > 32767 || value < -32768) println("Weird number[$i]: $value") else {
                                if (i < 64) {
                                    try {
                                        data[i] = receiveBufferMut.get(i).toFloat()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        data[i] = 2048.0f
                                    }
                                } else println(
                                    "Extra number[" + i + "]: " + receiveBufferMut.get(i).toFloat()
                                )
                            }
                        }
                        var dataVal = data[0].toString()
                        for (i in 1 until data.size){
                            dataVal += data[i].toString()
                        }

                        dataAvailable = true
                        receiveBufferMut = receiveBuffer.toMutableList()
                    }
                }

            }


            else if (MainActivity.READ_DATA_AVAILABLE == action){

                textView4.text = (intent.getByteArrayExtra(MainActivity.EXTRA_DATA)?.get(0)).toString()

                if (serviceUUID=="0000180f-0000-1000-8000-00805f9b34fb"){
                    var readData =
                        intent.getByteArrayExtra(MainActivity.EXTRA_DATA)?.get(0)?.toULong()
                            ?.and(0xFFu)?.plus(180u)
                    textView4.text = (readData).toString()
                    if (readData != null) {
                        firstBatteryLvl = readData.toFloat()
                    }
                }

//                    if (readData != null) {
//                        if (readData<0){
//                            readData.plus(180+256)
//                        }
//                        else (readData.plus(180))
//                    }
//                    textView4.text = (readData).toString()
//                }

                if (serviceUUID=="0000180a-0000-1000-8000-00805f9b34fb"){
                    textView4.text = intent.getByteArrayExtra(MainActivity.EXTRA_DATA)?.toString(Charsets.US_ASCII)
                }
                val tempByte = intent.getByteArrayExtra(MainActivity.EXTRA_DATA)?.get(0)
                if (tempByte != null) {
                    characterFirstRead = tempByte.toInt()
                    receiveBufferMut3.add(receiveBufferMut3.size,characterFirstRead)
                }
            }

//            else if (MainActivity.RSSI_DATA_AVAILABLE == action){
//                RSSI_text.text = RSSI.toString()
//            }

            messageHandler()

        }
    }

    private fun messageHandler() {
    }

    @SuppressLint("SourceLockedOrientationActivity", "MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportFragmentManager.beginTransaction().replace(R.id.nav_container,deviceStatusFragment()).commit()
        if (!initialized) {
            initGraph()
            initialized = true
        }

        var ECGCharUuid = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val gatt: BluetoothGatt = bluetoothGatt
        val ECGServiceUuid = UUID.fromString(serviceUUID)

        if (serviceUUID=="6e400001-b5a3-f393-e0a9-e50e24dcca9e") {
            ECGCharUuid = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        }
        else if (serviceUUID=="0000180f-0000-1000-8000-00805f9b34fb") {
            ECGCharUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        }
        else if (serviceUUID=="0000180d-0000-1000-8000-00805f9b34fb") {
            ECGCharUuid = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        }

        else if (serviceUUID=="0000180a-0000-1000-8000-00805f9b34fb") {
            ECGCharUuid = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
        }

        val ECGChar = gatt.getService(ECGServiceUuid).getCharacteristic(ECGCharUuid)
        if (!ECGChar.isWriteable()){markButtonDisable(button2)}
        if (!ECGChar.isNotifiable()){
            markButtonDisable(button3)
            runOnUiThread{
                brain_chart.visibility = View.INVISIBLE
            }
        }
        if (!ECGChar.isReadable()){
            markButtonDisable(button4)

        }


        backButton.setOnClickListener(){
            val resultIntent = Intent()
            disableNotifications(ECGChar)
            resultIntent.putExtra("some_key", "String data");
            handler.removeMessages(0)
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }

        button3.setOnCheckedChangeListener{_,isChecked ->
            if (isChecked){
            isCChecked = true
            enableNotifications(gatt,ECGChar)

            }
            else{ disableNotifications(ECGChar)
                isCChecked = false}

        }

        button4.setOnClickListener(){
            gatt.readCharacteristic(ECGChar)
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        val intentFilter = IntentFilter()
        intentFilter.addAction(MainActivity.ACTION_GATT_CONNECTED)
        intentFilter.addAction(MainActivity.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(MainActivity.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(MainActivity.ACTION_DATA_AVAILABLE)
        intentFilter.addAction(MainActivity.READ_DATA_AVAILABLE)
        intentFilter.addAction(MainActivity.RSSI_DATA_AVAILABLE)
        return intentFilter
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceiver)
    }

    fun initGraph() {

        brain_chart.setDrawGridBackground(false)

        // no description text
        brain_chart.setDescription("")

        // enable touch gestures
        brain_chart.setTouchEnabled(false)

        // enable scaling and dragging
        brain_chart.isDragEnabled = false
        brain_chart.setScaleEnabled(false)
        brain_chart.scaleY = 1.0f

        // if disabled, scaling can be done on x- and y-axis separately
        brain_chart.setPinchZoom(true)

        brain_chart.axisLeft.setDrawGridLines(false)
        brain_chart.axisRight.isEnabled = false
        brain_chart.xAxis.setDrawGridLines(false)
        brain_chart.xAxis.setDrawAxisLine(false)

        for (i in 0..29) {
            values2.add(Entry(-1.0f,i))
            xvalue2.add(i.toString())
        }

        for (i in 0..29) {
            values3.add(Entry(-1.0f,i))
            xvalue3.add(i.toString())
        }

        for (i in 0..2079) {
            values.add(Entry(-1.0f,i))
            xvalue.add(i.toString())
        }


        updateChart()

        handler.postDelayed(object : Runnable {
            override fun run() {
                handler.postDelayed(this, updatePeriod.toLong())
                updateRoutine()
            }
        }, updatePeriod.toLong())

    }


    private fun updateChart() {
        brain_chart.resetTracking()
        setData()
        brain_chart.invalidate()
    }

    private fun setData() {
        var set1 = LineDataSet(values, "ECG")
        // create a dataset and give it a type
        if (serviceUUID=="0000180f-0000-1000-8000-00805f9b34fb"){
             set1 = LineDataSet(values3, "ECG")
        }
        if (serviceUUID=="0000180d-0000-1000-8000-00805f9b34fb"){
            set1 = LineDataSet(values2, "ECG")
        }

        set1.color = R.color.teal_200
        set1.lineWidth = 1.0f
        set1.setDrawValues(false)
        set1.setDrawCircles(false)
        set1.setDrawFilled(false)

        var xvalueH = xvalue
        if (serviceUUID=="0000180f-0000-1000-8000-00805f9b34fb"){
            xvalueH = xvalue3
        }
        if (serviceUUID=="0000180d-0000-1000-8000-00805f9b34fb"){
            xvalueH = xvalue2
        }
        // create a data object with the data sets
        val data = LineData(xvalueH,set1)

        // set data
        brain_chart.data = data
        val l: Legend = brain_chart.legend
        l.isEnabled = false


    }

    private fun updateRoutine() {
        counter2+=1

//        if (!nextUpdateArrived and isCChecked and (receiveBufferMut3.size>0)){
//            if (index3 == 60) index3 = 0
//            values3[index3] = Entry(receiveBufferMut3[0].toFloat(),index3)
//            index3++
//            updateChart()
//        }
//
//        else if(nextUpdateArrived and isCChecked)
//        {
//            if (index3 == 60) index3 = 0
//            values3[index3] = Entry(data3[0],index3)
//            index3++
//            textView4.text = data3[0].toString()
//            updateChart()
//            nextUpdateArrived = false
//        }

        if (dataAvailable3){
            if (index3 == 30) index3 = 0
            if (data3[0]>=180){
                values3[index3] = Entry(data3[0],index3)
                previousEntry = data3[0]
                index3++
                updateChart()}
            dataAvailable3= false
        }
        else if (readingcounter3!=-1 && button3.isChecked == true) {
            readingcounter3++
        }
        else {
            previousEntry = firstBatteryLvl
            readingcounter3 = 0
        }

        if (readingcounter3==200 && !nextUpdateArrived){
            if (index3 == 30) index3 = 0
            values3[index3] = Entry(previousEntry,index3)
            index3++
            updateChart()
            readingcounter3=0
        }

        if (dataAvailable2){
            if (index2 == 30) index2 = 0
            values2[index2] = Entry(data2[0],index2)
            index2++
            updateChart()
            dataAvailable2 = false
        }
             //readingcounter2 ++

        if (dataAvailable){
            if (index == 2080) index = 0
            for (i in 0..39) {
                values[index] = Entry(data[i],index)
                index++
                updateChart()
                dataAvailable = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun enableNotifications(bluetoothGatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> {
                Log.e("ConnectionManager", "${characteristic.uuid} doesn't support notifications/indications")
                return
            }
        }

        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(bluetoothGatt, cccDescriptor, payload)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }
    @SuppressLint("MissingPermission")
    fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!characteristic.isNotifiable() && !characteristic.isIndicatable()) {
            Log.e("ConnectionManager", "${characteristic.uuid} doesn't support indications/notifications")
            return
        }

        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (bluetoothGatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.e("ConnectionManager", "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(bluetoothGatt,cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } ?: Log.e("ConnectionManager", "${characteristic.uuid} doesn't contain the CCC descriptor!")
    }

    @SuppressLint("MissingPermission")
    fun writeDescriptor(bluetoothGatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    fun BluetoothGattCharacteristic.isWriteable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
        properties and property != 0

    @SuppressLint("ResourceAsColor")
    fun markButtonDisable(button: Button) {
        button?.isEnabled = false
        button?.setTextColor(R.color.white)
        button?.setBackgroundColor(R.color.black)
    }


}


