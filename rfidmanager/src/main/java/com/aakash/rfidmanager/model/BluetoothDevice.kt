package com.aakash.rfidmanager.model

import com.aakash.rfidmanager.RFIDManager


data class BluetoothDevice(
    val deviceName: String,
    val deviceID: String,
    val deviceType: RFIDManager.DeviceType?,
    var battery: Int = 0,
    var readerPower: Int = 0,
    var connected: Boolean = false
)