package com.aakash.rfidmanager.bluetooth

import android.bluetooth.BluetoothDevice

interface IBluetooth {
    /**
     * When a new Device  is found
     * @param bluetoothDevice Found Device
     */
    fun deviceFound(bluetoothDevice: BluetoothDevice)

    /**
     * This callback is received when requested pairing is successful
     * @param  bluetoothDevice Device which is paired
     */
    fun pairingComplete(bluetoothDevice: BluetoothDevice)

    /**
     * This callback is received when requested pairing is failed
     * @param bluetoothDevice Device which not got successfully paired
     */
    fun pairingFailed(bluetoothDevice: BluetoothDevice)
}