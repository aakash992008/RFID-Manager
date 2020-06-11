package com.aakash.rfidmanager.bluetooth

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.uk.tsl.rfid.asciiprotocol.AsciiCommander
import com.uk.tsl.rfid.asciiprotocol.device.Reader
import com.uk.tsl.rfid.asciiprotocol.device.ReaderManager

object BluetoothManager {


    private var listener: IBluetooth? = null
    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()


    /**
     * FUNCTION FOR CALLBACKS FOR BLUETOOTH DEVICE FOUND,PAIRING SUCCESS,PARING FAILURE
     * @param listener Listener to receive callbacks for bluetooth activity
     * @param activity Activity Context to register Broadcast for bluetooth activity
     */
    fun setListener(listener: IBluetooth, activity: Activity) {
        this.listener = listener
        activity.registerReceiver(
            availableDevicesReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
        val intentFilter = IntentFilter(BluetoothDevice.EXTRA_BOND_STATE)
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        activity.registerReceiver(mParingRequests, intentFilter)
    }


    fun setListener(listener: IBluetooth) {
        this.listener = listener
    }

    /**
     * FIND NEARBY AVAILABLE DEVICES
     * @param activity Activity Context to register Broadcast
     */
    fun findAvailableDevices(activity: Activity) {
        bluetoothAdapter?.let {
            if (!bluetoothAdapter.isDiscovering)
                bluetoothAdapter.startDiscovery()
        }
        activity.registerReceiver(
            availableDevicesReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )
        val intentFilter = IntentFilter(BluetoothDevice.EXTRA_BOND_STATE)
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        activity.registerReceiver(mParingRequests, intentFilter)

    }


    fun findAvailableDevices() {
        bluetoothAdapter?.let {
            if (!bluetoothAdapter.isDiscovering)
                bluetoothAdapter.startDiscovery()

        }
    }

    /**
     * Returns Bluttoth Adapter
     */
    fun getBluetoothAdapter(): BluetoothAdapter? {
        return bluetoothAdapter
    }


    /**
     * Cancel Discovering Nearby Devices
     */
    fun cancelDiscovery() {
        bluetoothAdapter.cancelDiscovery()
    }


    /**
     * Enable Bluetooth  of Device
     */
    fun enableBluetooth() {
        bluetoothAdapter.enable()
    }

    /**
     * Check if the bluetooth is turned on or not
     * @return TRUE if Turned On
     * @return FALSE if Turned off
     */
    fun isEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }

    /**
     * Send a pairing request to a Bluetooth Device
     * @param bluetoothDevice Device that need to be paired
     */
    fun pairDevice(bluetoothDevice: BluetoothDevice) {
        bluetoothDevice.createBond()
    }


    /**
     * Unregister the registered Receivers
     * Call this function when you no longer need bluetooth callbacks
     * Best is to call this function in OnDestroy
     * @param activity Activity context to unregister
     */
    fun unregisterReceivers(activity: Activity) {
        listener = null
        try {
            activity.unregisterReceiver(availableDevicesReceiver)
        } catch (e: Exception) {

        }
        try {
            activity.unregisterReceiver(mParingRequests)
        } catch (e: Exception) {

        }
    }

    /**
     * Function Gives the list of paired RFID devices
     * @param context pass the current context
     */
    fun getPairedDevices(context: Context): List<Reader> {
        if (ReaderManager.sharedInstance() == null) {
            ReaderManager.create(context)
            AsciiCommander.createSharedInstance(context)
        }
        ReaderManager.sharedInstance().updateList()
        return ReaderManager.sharedInstance().readerList.list()
    }


    /** RECEIVER FOR GETTING AVAILABLE DEVICES */
    private val availableDevicesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_FOUND == intent.action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    if (it.name != null && it.name.isNotEmpty()) {
                        listener?.deviceFound(it)
                    }
                }
            }
        }
    }

    /** BROADCAST RECEIVER FOR CHECKING AND GETTING NEW PAIRED DEVICE  */
    private val mParingRequests = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val mDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            mDevice?.let {
                Log.i("Bluetooth", intent.action)
                Log.i("Bluetooth", "Bond State " + mDevice.bondState)
                if (it.bondState == BluetoothDevice.BOND_BONDED) {
                    /** IF PAIRING IS COMPLETED */
                    listener!!.pairingComplete(mDevice)
                } else {
                    if (it.bondState == BluetoothDevice.BOND_NONE) {
                        /** IF PAIRING IS FAILED */
                        listener!!.pairingFailed(mDevice)

                    }
                }
            }
        }
    }

}
