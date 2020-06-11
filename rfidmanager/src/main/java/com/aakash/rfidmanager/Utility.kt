package com.aakash.rfidmanager

object Utility {
    fun set24BitTagID(tagID: String): String {
        if (tagID.length > 24) {
            return tagID
        }
        val zeroes = StringBuilder()
        for (i in 1..24 - tagID.length) {
            zeroes.append("0")
        }
        return "${zeroes}${tagID}"
    }
}