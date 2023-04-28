package com.example.easyecg

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues.TAG
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.blekotlinepunchthrough.ServiceResultAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.nav_container
import kotlinx.android.synthetic.main.activity_main2.*
import kotlinx.android.synthetic.main.activity_main3.*
import kotlinx.android.synthetic.main.fragment_device_status.*
import java.util.*


private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val RUNTIME_PERMISSION_REQUEST_CODE = 2
lateinit var bluetoothGatt: BluetoothGatt
var serviceResults = mutableListOf<BluetoothGattService>()
var RSSI: Int = 0
var serviceName: String = "Service"
var serviceUUID: String = "UUID"
var serviceCharacteristic = mutableListOf<String>()
var Position: Int = 4

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {
    var num: Int = 0
    //val fragment = supportFragmentManager.findFragmentById(R.id.nav_container)

    //------------>> Scanning Related Declaration
    private var isScanning = false
    set(value) {
        field = value
        runOnUiThread { scanButton.text = if (value) "Stop Scan" else "Scan" }
    }
    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            if (isScanning) {
                stopBleScan()
            }

            bluetoothGatt = result.device.connectGatt(this, false, gattCallback)
            with(result.device){
                Log.w("ScanResultAdapter", "Connecting to $address")
            }
        }
    }


    private val serviceResultAdapter: ServiceResultAdapter by lazy {
        ServiceResultAdapter(serviceResults){result->
            val intent = Intent(this, MainActivity2::class.java)
            intent.putExtra("test_num",num)
            startActivityForResult(intent,1)

        }
    }

    //------------>> onActivity (when calling activity ask permission for bluetooth)
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }
    @SuppressLint("MissingPermission")
    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }
    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    //------------>> onCreate Activity
    @SuppressLint("SourceLockedOrientationActivity", "CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportFragmentManager.beginTransaction().replace(R.id.nav_container,deviceStatusFragment()).commit()

        scanButton.setOnClickListener {
            if (isScanning) {
                stopBleScan()
                scanResultView.visibility = View.GONE
            } else {
                scanResultView.visibility = View.VISIBLE

                //setupRecyclerView()
                startBleScan()
            }

        }



        disconnectButton.setOnClickListener{

            stopBleScan()
            bluetoothGatt.disconnect()
            bluetoothGatt.close()
            serviceResults.clear()
            runOnUiThread{
                scanButton.visibility = View.VISIBLE
                it.visibility = View.GONE
                serviceResultView.visibility = View.GONE
                userMode.visibility = View.GONE
            }

        }

        userMode.setOnClickListener {
            val intent = Intent(this, MainActivity3::class.java)
            intent.putExtra("test_num",num)
            startActivityForResult(intent,1)
        }
    }
    //------------>> Establish Bluetooth Connection
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            bluetoothGatt.readRemoteRssi()
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")


                    var bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt?.discoverServices()
                    }


                    runOnUiThread {

                        scanResultView.visibility = View.GONE
                        scanButton.visibility = View.GONE
                        nav_container.visibility = View.VISIBLE
                        if(deviceName_text != null){
                            address_text.text = gatt.device.address
                            deviceName_text.text = gatt.device.name
                            status_text.text = "connected"

                        }
                        //disconnectButton.visibility = View.VISIBLE
                        //deviceStatus.text = "connectedto $deviceAddress"
                        serviceResultView.visibility = View.VISIBLE
                        userMode.visibility = View.VISIBLE
//                        userButton.visibility = View.VISIBLE

                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    runOnUiThread(){
                        status_text.text = "disconnected"
                    }
                    broadcastUpdate(ACTION_GATT_DISCONNECTED,0)
                    gatt.close()
                }
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
            }

        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${value.toHexString()}")
                        broadcastUpdate(READ_DATA_AVAILABLE, characteristic.value);

                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }

        fun ByteArray.toHexString(): String =
            joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("tag123", String.format("BluetoothGatt ReadRssi[%d]", rssi))
                broadcastUpdate(READ_DATA_AVAILABLE, rssi);
                RSSI = rssi
                RSSI_text.text = RSSI.toString()

            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")

                printGattTable() // See implementation just above this section
                // Consider connection setup as complete here
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                broadcastUpdate(Companion.ACTION_DATA_AVAILABLE, characteristic.value);

            }
        }
    }
    private fun broadcastUpdate(
        action: String,
        characteristic: ByteArray
    ) {
        val intent = Intent(action)
        intent.putExtra(EXTRA_DATA, characteristic)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(
        action: String,
        characteristic: Int
    ) {
        val intent = Intent(action)
        intent.putExtra(EXTRA_DATA, characteristic)
        sendBroadcast(intent)
    }

    fun writeDescriptor(bluetoothGatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    fun enableNotifications( bluetoothGatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
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

    fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

    fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
        properties and property != 0

    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }

        services.forEach { service ->
            serviceResults.add(service)
            val characteristicsTable = service.characteristics.joinToString() { it.uuid.toString() }
            serviceCharacteristic.add(characteristicsTable)
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
        Log.i("printGattTable", "\nCharacteristics:\n$serviceCharacteristic")
        runOnUiThread {
            scanResultAdapter.notifyDataSetChanged()
            setupRecyclerView2()
        }
    }

    //------------>> Permission Checking and Requests
    @SuppressLint("NotifyDataSetChanged")
    private fun startBleScan() {
        setupRecyclerView()


        if (!hasRequiredRuntimePermissions()) {
            requestRelevantRuntimePermissions()
        }

        else if (!isScanning) {
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    private fun setupRecyclerView() {
        scanResultView.apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        val animator = scanResultView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }
    private fun setupRecyclerView2() {

        serviceResultView.apply {
            adapter = serviceResultAdapter
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }

        val animator2 = serviceResultView.itemAnimator
        if (animator2 is SimpleItemAnimator) {
            animator2.supportsChangeAnimations = false
        }
    }


    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Log.i("ScanCallback", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(result)
                if (scanResults.size == 1){ //because I added a header for the recycler view, the first result will disappear
                    scanResults.add(result)
                }
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("ScanCallback", "onScanFailed: code $errorCode")
        }
    }

    fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    //------------>> Permission Checking and Requests
    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun Context.hasRequiredRuntimePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun Activity.requestRelevantRuntimePermissions() {
        if (hasRequiredRuntimePermissions()) { return }
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                requestLocationPermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                requestBluetoothPermissions()
            }
        }
    }

    //my phone the location permission is granted together with bluetooth
    private fun requestLocationPermission() {
        val builder = AlertDialog.Builder(this)

        with(builder)
        {
            setTitle("Permission Request")
            setMessage("EasyECG require permission for location tracking to scan.")
            setCancelable(false)
            setPositiveButton("OK",positiveButtonClick1)
            show()
        }
    }

    @SuppressLint("NewApi")
    private fun requestBluetoothPermissions() {
        val builder = AlertDialog.Builder(this)

        with(builder)
        {
            setTitle("Permission Request")
            setMessage("EasyECG require permission for bluetooth to scan.")
            setCancelable(false)
            setPositiveButton("OK", DialogInterface.OnClickListener(function = positiveButtonClick2))
            show()
        }
    }

    val positiveButtonClick1 = { dialog: DialogInterface, which: Int ->
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            RUNTIME_PERMISSION_REQUEST_CODE
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    val positiveButtonClick2 = { dialog: DialogInterface, which: Int ->
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ),
            RUNTIME_PERMISSION_REQUEST_CODE
        )
    }
    companion object {
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val READ_DATA_AVAILABLE: String = "com.example.bluetooth.le.READ_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
        const val RSSI_DATA_AVAILABLE = "com.example.bluetooth.le.RSSI_DATA_AVAILABLE"
    }



}