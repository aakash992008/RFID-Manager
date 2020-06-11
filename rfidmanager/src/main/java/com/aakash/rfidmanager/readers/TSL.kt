package com.aakash.rfidmanager.readers

import android.util.Log
import com.aakash.rfidmanager.R
import com.aakash.rfidmanager.RFIDManager
import com.aakash.rfidmanager.RfidExceptions
import com.aakash.rfidmanager.Utility
import com.aakash.rfidmanager.model.BluetoothDevice
import com.uk.tsl.rfid.asciiprotocol.AsciiCommander
import com.uk.tsl.rfid.asciiprotocol.commands.AlertCommand
import com.uk.tsl.rfid.asciiprotocol.commands.BatteryStatusCommand
import com.uk.tsl.rfid.asciiprotocol.commands.InventoryCommand
import com.uk.tsl.rfid.asciiprotocol.commands.SwitchActionCommand
import com.uk.tsl.rfid.asciiprotocol.device.Reader
import com.uk.tsl.rfid.asciiprotocol.device.ReaderManager
import com.uk.tsl.rfid.asciiprotocol.enumerations.*
import com.uk.tsl.rfid.asciiprotocol.responders.ISwitchStateReceivedDelegate
import com.uk.tsl.rfid.asciiprotocol.responders.SwitchResponder

class TSL : RFIDManager() {


    private var connectDisconnectListener: RFIDConnectDisconnect? = null
    private var tslReader: Reader? = null
    private var mGeigerCounterResponder: InventoryCommand? = null
    private var mInventoryCommandResponder: InventoryCommand? = null
    private var mInventoryScanListener: InventoryScanListener? = null
    private var mInventoryCommand: InventoryCommand? = null
    private var geigerCounterListener: GeigerCounterListener? = null
    private var geigerTagID = ""

    override fun connectDisconnectDevice(
        bluetoothDevice: BluetoothDevice,
        listener: RFIDConnectDisconnect
    ) {
        connectDisconnectListener = listener
        ReaderManager.sharedInstance().updateList()
        for (reader: Reader in ReaderManager.sharedInstance().readerList.list()) {
            if (reader.displayInfoLine.contains(bluetoothDevice.deviceID)) {
                connectDisconnectReader(reader)
                return
            }
        }
    }


    private fun connectDisconnectReader(reader: Reader) {
        if (reader.isConnected) {
            reader.disconnect()
            connectDisconnectListener!!.disconnected()
            connectDisconnectListener!!.message(R.string.reader_disconnected)
            tslReader = null
        } else {
            reader.connect()
            android.os.Handler().postDelayed({
                if (reader.isConnected) {
                    getCommander().reader = reader
                    beep(TriState.YES)
                    bluetoothDevice?.connected = reader.isConnected
                    setReaderBattery()
                    connectDisconnectListener!!.connected()
                    connectDisconnectListener!!.message(R.string.reader_connected)
                    tslReader = reader
                } else {
                    manager = null
                    tslReader = null
                    connectDisconnectListener!!.message(R.string.unable_to_connect_reader)
                    connectDisconnectListener!!.disconnected()
                    bluetoothDevice?.connected = false
                }
            }, 4500)
        }
    }

    override fun setUpGeigerCounter(listener: GeigerCounterListener) {
        if (!getCommander().reader.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        geigerCounterListener = listener
        mGeigerCounterResponder = InventoryCommand()
        mGeigerCounterResponder?.let {
            it.includeEpc = TriState.YES
            it.includePC = TriState.YES
            it.setCaptureNonLibraryResponses(true)
        }


        mInventoryCommand = InventoryCommand()
        mInventoryCommand?.let {
            it.includeEpc = TriState.YES
            it.includePC = TriState.YES
            it.includeChecksum = TriState.YES
            it.includeTransponderRssi = TriState.YES
            it.inventoryOnly = TriState.NO
            it.selectOffset = 0x20

            // Use session with long persistence and select tags away from default state
            it.selectAction = SelectAction.DEASSERT_SET_B_NOT_ASSERT_SET_A
            it.selectTarget = SelectTarget.SESSION_2

            it.querySelect = QuerySelect.ALL
            it.querySession = QuerySession.SESSION_2
            it.queryTarget = QueryTarget.TARGET_B
            it.useAlert = TriState.NO
        }




        mGeigerCounterResponder?.setTransponderReceivedDelegate { transponderData, b ->
            geigerCounterListener?.let {
                if (transponderData.epc.contains(geigerTagID)) {
                    if (transponderData.rssi == null) {
                        return@setTransponderReceivedDelegate
                    }
                    var data = (100.00 + transponderData.rssi!!.toFloat() - 15.00).toFloat()
                    data = data / 45 * 100
                    if (data > 100) {
                        data = 100f
                    }
                    beep(TriState.NO)
                    it.distancePercentage(data.toInt())
                }
            }
        }

        getCommander().addSynchronousResponder()

        val saCommand = SwitchActionCommand.synchronousCommand()
        /// Enable asynchronous switch state reporting
        saCommand.asynchronousReportingEnabled = TriState.YES
        // Disable the default switch actions
        saCommand.singlePressAction = SwitchAction.INVENTORY
        saCommand.doublePressAction = SwitchAction.OFF
        getCommander().executeCommand(saCommand)

        // Use the SwitchResponder to monitor asynchronous switch reports
        val switchResponder = SwitchResponder()
        switchResponder.switchStateReceivedDelegate = mSwitchDelegate
        getCommander().addResponder(switchResponder)
    }

    override fun startGeigerCounter(tagID: String) {
        if (tagID.isEmpty()) {
            throw RfidExceptions(RfidExceptions.EMPTY_TAG_ID)
        } else if (tagID.length > 24) {
            throw RfidExceptions(RfidExceptions.INVALID_TAG_ID)
        } else if (!getCommander().reader.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        geigerTagID = Utility.set24BitTagID(tagID)
        getCommander().addResponder(mGeigerCounterResponder)
        getCommander().executeCommand(mInventoryCommand)
    }

    override fun stopGeigerCounter() {
        if (!getCommander().reader.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        getCommander().removeResponder(mGeigerCounterResponder)
    }

    override fun removeListeners() {
        geigerCounterListener = null
    }

    private fun beep(vibration: TriState) {
        if (!getCommander().reader.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        val alertCommand = AlertCommand()
        alertCommand.tone = BuzzerTone.HIGH
        alertCommand.duration = AlertDuration.SHORT
        alertCommand.enableVibrator = vibration
        getCommander().executeCommand(alertCommand)
    }

    private fun getCommander(): AsciiCommander {
        return AsciiCommander.sharedInstance()!!
    }

    override fun disconnectReader() {
        if (tslReader == null || !tslReader!!.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        tslReader?.let {
            if (it.isConnected) {
                it.disconnect()
                connectDisconnectListener?.disconnected()
            }
        }
        manager = null
    }

    override fun setReaderBattery() {
        getCommander()?.let {
            if (it.reader.isConnected) {
                val bCommand = BatteryStatusCommand.synchronousCommand()
                try {
                    getCommander().clearResponders()
                    getCommander().executeCommand(bCommand)
                } catch (e: Exception) {
                    Log.d("BATTERY EXCEPTION TSL", "" + e.localizedMessage!!)
                }

                val batteryLevel = bCommand.batteryLevel
                bluetoothDevice?.battery = batteryLevel
                Log.d("TSL BATTERY LEVEL", "" + batteryLevel)
                getCommander().reader.deviceProperties
            }
        }

    }


    private val mSwitchDelegate: ISwitchStateReceivedDelegate = (ISwitchStateReceivedDelegate {
        if (SwitchState.SINGLE == it) {
            geigerCounterListener?.triggerPressed()
            mInventoryScanListener?.triggerPressed()
            Log.d("TRIGGER PRESSED TSL", "SINGLE")
        } else if (SwitchState.DOUBLE == it) {
            Log.d("TRIGGER PRESSED TSL", "DOUBLE")
        } else if (SwitchState.OFF == it) {
            Log.d("TRIGGER PRESSED TSL", "RELEASED")
            geigerCounterListener?.triggerReleased()
            mInventoryScanListener?.triggerReleased()
        }
    })


    override fun setUpInventoryScan(listener: InventoryScanListener) {
        if (!getCommander().reader.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        mInventoryScanListener = listener
        getCommander().clearResponders()
        getCommander().addSynchronousResponder()

        mInventoryCommandResponder = InventoryCommand()
        mInventoryCommandResponder?.let {
            it.resetParameters = TriState.YES
            it.includeTransponderRssi = TriState.YES
            it.includeChecksum = TriState.YES
            it.includeEpc = TriState.YES
            it.includeDateTime = TriState.YES
            it.setCaptureNonLibraryResponses(true)
        }


        mInventoryCommand = InventoryCommand()
        mInventoryCommand?.let {
            it.resetParameters = TriState.YES
            it.includeTransponderRssi = TriState.YES
            it.includeChecksum = TriState.YES
            it.includeEpc = TriState.YES
            it.includeDateTime = TriState.YES
            it.resetParameters = TriState.YES
            it.includeTransponderRssi = TriState.YES
            it.includeChecksum = TriState.YES
            it.includeEpc = TriState.YES
            it.includeDateTime = TriState.YES
        }





        mInventoryCommandResponder?.setTransponderReceivedDelegate { transponderData, b ->
            listener?.let {
                it.tagRead(transponderData.epc)
            }
        }

        val saCommand = SwitchActionCommand.synchronousCommand()
        /// Enable asynchronous switch state reporting
        saCommand.asynchronousReportingEnabled = TriState.YES
        // Disable the default switch actions
        saCommand.singlePressAction = SwitchAction.INVENTORY
        saCommand.doublePressAction = SwitchAction.OFF
        getCommander().executeCommand(saCommand)

        // Use the SwitchResponder to monitor asynchronous switch reports

        // Use the SwitchResponder to monitor asynchronous switch reports
        val switchResponder = SwitchResponder()
        switchResponder.switchStateReceivedDelegate = mSwitchDelegate
        getCommander().addResponder(switchResponder)

    }

    override fun startInventory() {
        if (!getCommander().reader.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        getCommander().addSynchronousResponder()
        getCommander().addResponder(mInventoryCommandResponder)
        getCommander().executeCommand(mInventoryCommand)

    }

    override fun stopInventory() {
        if (!getCommander().reader.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        try {
            getCommander()?.let {
                mInventoryCommandResponder?.let {
                    getCommander().removeResponder(mInventoryCommandResponder)
                }
            }
        } catch (e: Exception) {

        }

    }


}