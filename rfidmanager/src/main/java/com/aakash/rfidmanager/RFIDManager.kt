package com.aakash.rfidmanager

import com.aakash.rfidmanager.model.BluetoothDevice

/**
 * THIS IS AN ABSTRACT CLASS WHICH CAN'T BE INITIALISED.
 * Initialise it through reader classes ZEBRA OR TSL OBJECT
 **/
abstract class RFIDManager {

    enum class DeviceType(val deviceType: String) {
        ZEBRA("ZEBRA"), TSL("TSL");
    }


    companion object {
        var bluetoothDevice: BluetoothDevice? = null
        var manager: RFIDManager? = null
    }

    /**
     * Function to automatically connect or disconnect a Bluetooth RFID reader device
     * @param bluetoothDevice Device which need to be connected or disconnected
     * @param listener Listener to listen for callbacks that reader got connected or not
     */
    abstract fun connectDisconnectDevice(
        bluetoothDevice: BluetoothDevice,
        listener: RFIDConnectDisconnect
    )


    @Throws(RfidExceptions::class)
    /**
     * This function should be called before using starting Geiger Counter
     * @param listener Listener to listen for Distance callbacks
     */
    abstract fun setUpGeigerCounter(listener: GeigerCounterListener)

    @Throws(RfidExceptions::class)
    /**
     * Function to start geiger counter for reader
     * @param tagID tagID that you want to search (TAG_ID is converted into 24length string at this point)
     */
    abstract fun startGeigerCounter(tagID: String)

    @Throws(RfidExceptions::class)
    /**
     * Function to stop Geiger Counter
     */
    abstract fun stopGeigerCounter()


    @Throws(RfidExceptions::class)
    abstract fun setUpInventoryScan(listener: InventoryScanListener)

    @Throws(RfidExceptions::class)
    abstract fun startInventory()

    @Throws(RfidExceptions::class)
    abstract fun stopInventory()


    @Throws(RfidExceptions::class)
    abstract fun removeListeners()

    @Throws(RfidExceptions::class)
    abstract fun disconnectReader()

    @Throws(RfidExceptions::class)
    abstract fun setReaderBattery()


    interface RFIDConnectDisconnect {
        fun connected()
        fun disconnected()
        fun message(id: Int)
    }

    interface GeigerCounterListener {
        fun distancePercentage(percentage: Int)
        fun message(message: String)
        fun triggerPressed()
        fun triggerReleased()
    }

    interface InventoryScanListener {
        fun tagRead(tagID: String)
        fun triggerPressed()
        fun triggerReleased()
        fun message(message: String)
    }

}