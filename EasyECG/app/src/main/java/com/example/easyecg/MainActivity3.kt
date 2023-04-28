package com.example.easyecg

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.*
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.activity_main3.*
import kotlinx.android.synthetic.main.activity_main3.brain_chart2
import kotlinx.android.synthetic.main.fragment_device_status.*
import okhttp3.*
import java.io.IOException
import java.util.*

private var characterFirstRead: Int = 0

private var dataAvailable = false
private var dataAvailable2 = false
private var nextUpdateArrived = false

private val data = FloatArray(122)

private val data2 = FloatArray(2)
private val data3 = FloatArray(2)
private var counter2 = 0
private var isCChecked = false


class MainActivity3 : AppCompatActivity() {


    var ECGServiceUuid1 = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    var ECGServiceUuid2 = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    var ECGServiceUuid3 = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")

    var ECGCharUuid1 = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    var ECGCharUuid2 = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    var ECGCharUuid3 = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

    val gatt1: BluetoothGatt = bluetoothGatt
    val gatt2: BluetoothGatt = bluetoothGatt
    val gatt3: BluetoothGatt = bluetoothGatt

    val ECGChar1 = gatt1.getService(ECGServiceUuid1).getCharacteristic(ECGCharUuid1)
    val ECGChar2 = gatt2.getService(ECGServiceUuid2).getCharacteristic(ECGCharUuid2)
    val ECGChar3 = gatt3.getService(ECGServiceUuid3).getCharacteristic(ECGCharUuid3)

    private var initialized = false
    val xvalue = ArrayList<String>()
    var values = ArrayList<Entry>()
    val xvalue2 = ArrayList<String>()
    var values2 = ArrayList<Entry>()
    val xvalue3 = ArrayList<String>()
    var values3 = ArrayList<Entry>()

    private val updatePeriod = 4

    private var index = 0
    private var index2 = 0
    private var index3 = 0

    private var postData = ArrayList<Float>()
    private var postIndex = 0
    val notPosting = true
    //var dataClear = false
    var dataClear = true

    val handler: Handler = Handler()

    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            val action = intent.action
            if (MainActivity.ACTION_GATT_DISCONNECTED.equals(action)) {
                statusText2.text = "disconnected"
            }
            if (MainActivity.ACTION_DATA_AVAILABLE.equals(action)) {
                val value = intent.getByteArrayExtra(MainActivity.EXTRA_DATA)
                if(serviceUUID=="0000180f-0000-1000-8000-00805f9b34fb" && value != null){
                    //                    value.forEach { reading->
//                        Log.i("heartrate reading", "type is ${reading::class.simpleName} heart-rate is $reading")
//                    }
                    //Log.i("heartrate", "type is ${value[1].toUInt()::class.simpleName} heart-rate is ${value[1].toUInt()}")
                    val buffer: ULong = 0u
                    var heartRate = (buffer and 0xFFu) shl 8 or value[1].toULong() and 0xFFu
                    var heartRateInt = heartRate.toInt()
                    if (heartRateInt < 0){
                        heartRateInt += 128
                    }
                    //textView16.text = heartRateInt.toString()
                    receiveBufferMut2.add(receiveBufferMut2.size, heartRateInt)
                    Log.i("heartrate", "type is ${heartRate::class.simpleName + 128} heart-rate is ${heartRate}")

                    //dataAvailable2 = true

                    var counter: Int = 0
                    receiveBufferMut2.forEach(){heartRateReading->
                        data2[counter] = heartRateReading.toFloat()
                        counter++
                    }
                    receiveBufferMut2 = receiveBuffer.toMutableList()
                }

                else if(serviceUUID=="0000180d-0000-1000-8000-00805f9b34fb" && value != null) {
//                    value.forEach { reading->
//                        Log.i("heartrate reading", "type is ${reading::class.simpleName} heart-rate is $reading")
//                    }
                    //Log.i("heartrate", "type is ${value[1].toUInt()::class.simpleName} heart-rate is ${value[1].toUInt()}")
                    val buffer: ULong = 0u
                    var heartRate = (buffer and 0xFFu) shl 8 or value[1].toULong() and 0xFFu
                    var heartRateInt = heartRate.toInt()
                    if (heartRateInt < 0){
                        heartRateInt += 128
                    }
                    //textView14.text = heartRateInt.toString()
                    receiveBufferMut2.add(receiveBufferMut2.size, heartRateInt)
                    Log.i("heartrate", "type is ${heartRate::class.simpleName + 128} heart-rate is ${heartRate}")

                    //dataAvailable2 = true

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

//                        textView13.text = value[120].toString()
//                        textView10.text = value[121].toString()
//                        textView11.text = value[122].toString()



                        val heartRate =  (buffer and 0xFFu) shl 8 or value[241].toULong() and 0xFFu
                        var heartRateInt = heartRate.toInt()
                        if (heartRateInt < 0){
                            heartRateInt += 128
                        }
                        val heartrateReading = heartRateInt.toString()
                        val textView14Txt= heartrateReading+" bpm"
                        textView14.text = textView14Txt

                        val batterylevel =  (buffer and 0xFFu) shl 8 or value[240].toULong() and 0xFFu
                        var batterylevelInt = batterylevel.toInt()
                        if (batterylevelInt < 0){
                            batterylevelInt += 128
                        }
                        batterylevelInt += 180
                        val voltageReading = batterylevelInt.toString()
                        val textView16Txt= "   "+voltageReading.subSequence(0,1).toString()+"."+voltageReading.subSequence(1,3)+" volt"
                        textView16.text = textView16Txt

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
                        //receiveBufferMut.size
                        for (i in 0 until 40) {
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
                        //receiveBufferMut = receiveBuffer.toMutableList()
                        if (dataClear == true){
                            receiveBufferMut.subList(0,40).clear()
                        }
                    }
                }

            }


            if (MainActivity.READ_DATA_AVAILABLE == action){
                intent.getByteArrayExtra(MainActivity.EXTRA_DATA)?.get(0)
                val voltageReading = (intent.getByteArrayExtra(MainActivity.EXTRA_DATA)?.get(0)).toString()
                val textView16Txt= voltageReading.subSequence(0,1).toString()+"."+voltageReading.subSequence(1,3)+" volt"
                textView16.text = textView16Txt


                if (serviceUUID=="0000180a-0000-1000-8000-00805f9b34fb"){
                    textView14.text = intent.getByteArrayExtra(MainActivity.EXTRA_DATA)?.toString(Charsets.US_ASCII)
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


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        //requestWindowFeature(Window.FEATURE_NO_TITLE)
//        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)
        supportActionBar?.hide()

        postButton.setOnClickListener{
            val URL: String = "http://192.168.50.231:5000/post"
            //val URL: String = "http://172.20.10.4:5000/post"

            if (URL.isNotEmpty()){
                val okHttpClient = OkHttpClient()
                val formBody = FormBody.Builder()
                    .add("value", postData.toString())
                    .add("value2", postIndex.toString())
                    .build()
                val request = Request.Builder()
                    .url(URL)
                    .post(formBody)
                    .build()

                okHttpClient.newCall(request).enqueue(object: Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.i("response", "response failed")
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Log.i ("response", "response received")
                        val body = response?.body?.string()
                        runOnUiThread {
                            //postText.text = body
                            postRequestSuccessNotification()
                        }

                    }
                })
            }

        }

        button6.setOnClickListener {
            val resultIntent = Intent()
            disableNotifications(ECGChar1)
            resultIntent.putExtra("some_key", "String data");
            handler.removeMessages(0)
            setResult(Activity.RESULT_OK, resultIntent);
            finish()
        }

        if (!initialized) {
            initGraph()
            initialized = true
        }
        enableNotifications(gatt1,ECGChar1)

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

        brain_chart2.setDrawGridBackground(false)

        // no description text
        brain_chart2.setDescription("")

        // enable touch gestures
        brain_chart2.setTouchEnabled(false)

        // enable scaling and dragging
        brain_chart2.isDragEnabled = false
        brain_chart2.setScaleEnabled(false)
        brain_chart2.scaleY = 1.0f

        // if disabled, scaling can be done on x- and y-axis separately
        brain_chart2.setPinchZoom(true)

        brain_chart2.axisLeft.setDrawGridLines(false)
        brain_chart2.axisRight.isEnabled = false
        brain_chart2.xAxis.setDrawGridLines(false)
        brain_chart2.xAxis.setDrawAxisLine(false)
        brain_chart2.xAxis.position = XAxis.XAxisPosition.BOTTOM

        for (i in 0..59) {
            values2.add(Entry(-1.0f,i))
            xvalue2.add(i.toString())
        }

        for (i in 0..59) {
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
        brain_chart2.resetTracking()
        setData()
        brain_chart2.invalidate()
    }

    private fun setData() {
        var set1 = LineDataSet(values, "ECG")
        // create a dataset and give it a type


        set1.color = R.color.teal_200
        set1.lineWidth = 1.0f
        set1.setDrawValues(false)
        set1.setDrawCircles(false)
        set1.setDrawFilled(false)

        var xvalueH = xvalue
        // create a data object with the data sets
        val data = LineData(xvalueH,set1)

        // set data
        brain_chart2.data = data
        val l: Legend = brain_chart2.legend
        l.isEnabled = false


    }

    private fun postRequestSuccessNotification() {
        val builder = AlertDialog.Builder(this)

        with(builder)
        {
            setTitle("Data is uploaded to the server")
            setMessage("ECG data is posted successfully via http request")
            setCancelable(true)
            show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateRoutine() {
        counter2+=1

        if (!nextUpdateArrived and isCChecked and (receiveBufferMut3.size>0)){
            if (index3 == 60) index3 = 0
            values3[index3] = Entry(receiveBufferMut3[0].toFloat(),index3)
            index3++
            updateChart()
        }

        else if(nextUpdateArrived and isCChecked)
        {
            if (index3 == 60) index3 = 0
            values3[index3] = Entry(data3[0],index3)
            index3++
            //textView4.text = data3[0].toString()
            updateChart()
            nextUpdateArrived = false
        }

        if (dataAvailable2){
            if (index2 == 60) index2 = 0
            values2[index2] = Entry(data2[0],index2)
            index2++
            updateChart()
            dataAvailable2 = false
        }
        if (dataAvailable){
            if (index == 2080) {
                index = 0
            }

            for (i in 0..39) {
                values[index] = Entry(data[i],index)
                if (notPosting && postData.size < 10240) {
                    postData.add(data[i])
                }
                else if(notPosting){
                    postData[postIndex] = data[i]
                    postIndex += 1
                    if (postIndex == 10240) {
                        runOnUiThread {
                            postText.text = "Data is ready"
                        }
                        postIndex = 0
                    }
                }
                index++
                updateChart()
                dataAvailable = false
            }
            dataClear = true
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