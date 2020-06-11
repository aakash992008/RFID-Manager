package com.aakash.rfidmanager.readers

import android.os.AsyncTask
import android.util.Log
import com.aakash.rfidmanager.RFIDManager
import com.aakash.rfidmanager.RfidExceptions
import com.aakash.rfidmanager.Utility
import com.aakash.rfidmanager.model.BluetoothDevice
import com.zebra.rfid.api3.*

class Zebra : RFIDManager() {

    override fun connectDisconnectDevice(
        bluetoothDevice: BluetoothDevice,
        listener: RFIDConnectDisconnect
    ) {
        connectDisconnectListener = listener
        val readers = Readers()
        val pairedDevices = readers.GetAvailableRFIDReaderList()
        if (zebraReader != null) {
            if (zebraReader!!.isConnected) {
                ConnectDisconnectReader().execute(zebraReader)
                return
            }
        }
        for (readerDevice: ReaderDevice in pairedDevices) {
            if (readerDevice.address == bluetoothDevice.deviceID) {
                ConnectDisconnectReader().execute(readerDevice.rfidReader)
            }
        }
    }

    companion object {
        private var connectDisconnectListener: RFIDConnectDisconnect? = null
        private var geigerCounterListener: GeigerCounterListener? = null
        private var zebraReader: RFIDReader? = null
        private var zebraEvents: EventListener? = null
        private var mInventoryScanListener: InventoryScanListener? = null

        private val TRIGGER_RELEASED_EVENT = "HANDHELD_TRIGGER_RELEASED"
        private val TRIGGER_PRESSED_EVENT = "HANDHELD_TRIGGER_PRESSED"
        private val TRIGGER_EVENT = "HANDHELD_TRIGGER_EVENT"
        private val BUFFER_FULL_EVENT = "BUFFER_FULL_EVENT"
        val DISCONNECT_EVENT = "DISCONNECTION_EVENT"
        private val BATTERY_EVENT = "BATTERY_EVENT"
        private val TEMPERATURE_ALARM_EVENT = "TEMPERATURE_ALARM_EVENT"


        private class ConnectDisconnectReader : AsyncTask<RFIDReader, Void, RFIDReader>() {
            override fun doInBackground(vararg p0: RFIDReader?): RFIDReader {
                val reader = p0[0]
                if (reader!!.isConnected) {
                    try {
                        reader.disconnect()
                    } catch (e: InvalidUsageException) {
                        e.printStackTrace()
                    } catch (e: OperationFailureException) {
                        e.printStackTrace()
                    }
                } else {
                    try {
                        reader.connect()
                    } catch (e: InvalidUsageException) {
                        e.printStackTrace()
                    } catch (e: OperationFailureException) {
                        e.printStackTrace()
                    }
                }
                return reader
            }

            override fun onPostExecute(reader: RFIDReader?) {
                super.onPostExecute(reader)
                bluetoothDevice!!.connected = reader!!.isConnected
                if (reader.isConnected) {
                    if (zebraEvents == null) {
                        zebraEvents = EventListener()
                    }
                    zebraReader = reader
                    zebraReader?.Events?.setBatteryEvent(true)
                    zebraReader?.Events?.setReaderDisconnectEvent(true)
                    setReaderBattery()
                    connectDisconnectListener!!.connected()
                    try {
                        zebraReader?.Events?.addEventsListener(zebraEvents)
                    } catch (e: InvalidUsageException) {
                        e.printStackTrace()
                    } catch (e: OperationFailureException) {
                        e.printStackTrace()
                    }
                } else {
                    connectDisconnectListener!!.disconnected()
                    manager = null
                    zebraReader = null
                }

            }
        }


        class EventListener : RfidEventsListener {
            override fun eventReadNotify(rfidReadEvents: RfidReadEvents) {
                geigerCounterListener?.let {
                    val myTags = zebraReader?.Actions?.getReadTags(100)
                    if (myTags != null) {
                        for (tagData in myTags) {
                            if (tagData.isContainsLocationInfo) {
                                it.distancePercentage(tagData.LocationInfo.relativeDistance.toInt())
                            }
                        }
                    }
                }
            }

            override fun eventStatusNotify(rfidStatusEvents: RfidStatusEvents) {
                Log.d("ZEBRA EVENT", "" + rfidStatusEvents.StatusEventData.statusEventType)
                if (rfidStatusEvents.StatusEventData.statusEventType.toString() == DISCONNECT_EVENT) {
                    bluetoothDevice = null
                    connectDisconnectListener?.disconnected()
                    zebraReader = null
                    manager = null
                }

                geigerCounterListener?.let {
                    if (rfidStatusEvents.StatusEventData.statusEventType.toString() == TRIGGER_EVENT) {
                        if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.handheldEvent.toString() == TRIGGER_RELEASED_EVENT) {
                            it.triggerReleased()
                        } else if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.handheldEvent.toString() == TRIGGER_PRESSED_EVENT) {
                            it.triggerPressed()
                        }
                    }
                }


                if (rfidStatusEvents.StatusEventData.statusEventType.toString() == DISCONNECT_EVENT) {

                } else if (rfidStatusEvents.StatusEventData.statusEventType.toString() == TEMPERATURE_ALARM_EVENT) {

                } else if (rfidStatusEvents.StatusEventData.statusEventType.toString() == BATTERY_EVENT) {
                    bluetoothDevice?.battery = rfidStatusEvents.StatusEventData.BatteryData.level
                } else if (rfidStatusEvents.StatusEventData.statusEventType.toString() == BUFFER_FULL_EVENT) {
                } else if (rfidStatusEvents.StatusEventData.statusEventType.toString() == TRIGGER_EVENT) {
                    if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.handheldEvent.toString() == TRIGGER_RELEASED_EVENT) {
                    } else if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.handheldEvent.toString() == TRIGGER_PRESSED_EVENT) {
                    }
                }
            }
        }

        private fun setReaderBattery() {
            zebraReader?.let {
                if (it.isConnected) {
                    try {
                        it.Config.getDeviceStatus(true, false, false)
                    } catch (e: InvalidUsageException) {
                        e.printStackTrace()
                    } catch (e: OperationFailureException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    override fun setUpGeigerCounter(listener: GeigerCounterListener) {
        if (zebraReader == null) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        } else if (!zebraReader!!.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        geigerCounterListener = listener
        zebraReader?.let {
            it.Events.setHandheldEvent(true)
            it.Events.setTagReadEvent(true)
        }
    }

    override fun startGeigerCounter(tagID: String) {
        if (zebraReader == null) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        } else if (!zebraReader!!.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        zebraReader?.let {
            if (tagID.isEmpty()) {
                throw RfidExceptions(RfidExceptions.EMPTY_TAG_ID)
            } else if (tagID.length > 24) {
                throw RfidExceptions(RfidExceptions.INVALID_TAG_ID)
            } else if (!it.isConnected) {
                throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
            }
            it.Actions.TagLocationing.Perform(Utility.set24BitTagID(tagID), null, null)
        }
    }

    override fun stopGeigerCounter() {
        if (zebraReader == null) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        } else if (!zebraReader!!.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        zebraReader?.let {
            if (!it.isConnected) {
                throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
            }
            it.Actions.TagLocationing.Stop()
        }
    }


    override fun removeListeners() {
        connectDisconnectListener = null
        geigerCounterListener = null
    }

    override fun disconnectReader() {
        if (zebraReader == null) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        } else if (!zebraReader!!.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        zebraReader?.let {
            if (it.isConnected) {
                it.disconnect()
                connectDisconnectListener?.disconnected()
            }
            manager = null
            bluetoothDevice = null
        }
    }

    override fun setReaderBattery() {
        if (zebraReader == null) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        } else if (!zebraReader!!.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        zebraReader?.let {
            if (it.isConnected) {
                try {
                    it.Config.getDeviceStatus(true, false, false)
                } catch (e: InvalidUsageException) {
                    e.printStackTrace()
                } catch (e: OperationFailureException) {
                    e.printStackTrace()
                }
            }
        }
    }


    override fun setUpInventoryScan(listener: InventoryScanListener) {
        if (zebraReader == null) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        } else if (!zebraReader!!.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        zebraReader?.let {
            if (it.isConnected) {
                geigerCounterListener = null
                mInventoryScanListener = listener
                it.Events.setTagReadEvent(true)
                it.Events.setHandheldEvent(true)
                it.Events.setBatteryEvent(true)
                it.Events.setInventoryStartEvent(true)
                it.Events.setInventoryStopEvent(true)

                try {
                    it.Events.addEventsListener(zebraEvents)
                } catch (e: InvalidUsageException) {
                    listener.message(e.vendorMessage)
                    e.printStackTrace()
                } catch (e: OperationFailureException) {
                    throw RfidExceptions(
                        RfidExceptions.OPERATION_FAILURE,
                        e.statusDescription,
                        e.cause.toString()
                    )
                }

            } else {
                throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
            }
        }

    }

    override fun startInventory() {
        if (zebraReader == null) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        } else if (!zebraReader!!.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        zebraReader?.let {
            if (it.isConnected) {
                try {
                    it.Actions.Inventory.perform()
                } catch (e: InvalidUsageException) {
                    mInventoryScanListener?.message(e.vendorMessage)
                    e.printStackTrace()
                } catch (e: OperationFailureException) {
                    throw RfidExceptions(
                        RfidExceptions.OPERATION_FAILURE,
                        e.statusDescription,
                        e.cause.toString()
                    )
                }

            } else {
                throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
            }
        }
    }

    override fun stopInventory() {
        if (zebraReader == null) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        } else if (!zebraReader!!.isConnected) {
            throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
        }
        zebraReader?.let {
            if (it.isConnected) {
                try {
                    it.Actions.Inventory.stop()
                } catch (e: InvalidUsageException) {
                    mInventoryScanListener?.message(e.vendorMessage)
                    e.printStackTrace()
                } catch (e: OperationFailureException) {
                    mInventoryScanListener?.message("Operation Fail")
                    e.printStackTrace()
                }
            } else {
                throw RfidExceptions(RfidExceptions.READER_NOT_CONNECTED)
            }
        }
    }


}