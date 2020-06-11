package com.aakash.rfidmanager

class RfidExceptions : RuntimeException {

    var detailedMessage = ""
    var exceptionCode = 0
    var exceptionCause = ""

    constructor(errorCode: Int, message: String? = "", errorCause: String? = "") : super(message) {
        setParameters(errorCode);
    }

    private fun setParameters(errorCode: Int) {
        when (errorCode) {
            EMPTY_TAG_ID -> {
                detailedMessage = "Tag ID is Empty"
                exceptionCode = EMPTY_TAG_ID
            }
            INVALID_TAG_ID -> {
                detailedMessage = "Tag ID is Invalid"
                exceptionCode = INVALID_TAG_ID
            }
            READER_NOT_CONNECTED -> {
                detailedMessage = "Reader not connected"
                exceptionCode = READER_NOT_CONNECTED
            }
        }
    }


    companion object {
        val EMPTY_TAG_ID = 43534
        val INVALID_TAG_ID = 6757
        val READER_NOT_CONNECTED = 45768
        val OPERATION_FAILURE = 678
    }
}