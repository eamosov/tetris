package ru.efreet.trading.utils

import it.unimi.dsi.fastutil.bytes.ByteArrayList

fun ByteArrayList.getByteAsInt(index: Int): Int {
    return getByte(index).toInt() and 0x000000FF
}

fun ByteArrayList.setByteAsInt(index: Int, value: Int) {
    set(index, value.toByte())
}


fun ByteArrayList.setShort(index: Int, v: Int): Int {
    setByteAsInt(index, v.ushr(8))
    setByteAsInt(index + 1, v.ushr(0))
    return 2
}

fun ByteArrayList.getShort(index: Int): Short {
    val ch1 = getByteAsInt(index)
    val ch2 = getByteAsInt(index + 1)
    return ((ch1 shl 8) + (ch2 shl 0)).toShort()
}


fun ByteArrayList.setInt(index: Int, v: Int): Int {
    setByteAsInt(index, v.ushr(24))
    setByteAsInt(index + 1, v.ushr(16))
    setByteAsInt(index + 2, v.ushr(8))
    setByteAsInt(index + 3, v.ushr(0))
    return 4
}

fun ByteArrayList.getInt(index: Int): Int {
    val ch1 = getByteAsInt(index)
    val ch2 = getByteAsInt(index + 1)
    val ch3 = getByteAsInt(index + 2)
    val ch4 = getByteAsInt(index + 3)
    return (ch1 shl 24) + (ch2 shl 16) + (ch3 shl 8) + (ch4 shl 0)
}


fun ByteArrayList.setLong(index: Int, v: Long): Int {
    set(index, v.ushr(56).toByte())
    set(index + 1, v.ushr(48).toByte())
    set(index + 2, v.ushr(40).toByte())
    set(index + 3, v.ushr(32).toByte())
    set(index + 4, v.ushr(24).toByte())
    set(index + 5, v.ushr(16).toByte())
    set(index + 6, v.ushr(8).toByte())
    set(index + 7, v.ushr(0).toByte())
    return 8
}

fun ByteArrayList.getLong(index: Int): Long {
    val ch1 = getByteAsInt(index).toLong()
    val ch2 = getByteAsInt(index + 1).toLong()
    val ch3 = getByteAsInt(index + 2).toLong()
    val ch4 = getByteAsInt(index + 3).toLong()
    val ch5 = getByteAsInt(index + 4).toLong()
    val ch6 = getByteAsInt(index + 5).toLong()
    val ch7 = getByteAsInt(index + 6).toLong()
    val ch8 = getByteAsInt(index + 7).toLong()
    return (ch1 shl 56) + (ch2 shl 48) + (ch3 shl 40) + (ch4 shl 32) + (ch5 shl 24) + (ch6 shl 16) + (ch7 shl 8) + (ch8 shl 0)
}


fun ByteArrayList.setFloat(index: Int, v: Float): Int {
    return setInt(index, java.lang.Float.floatToIntBits(v))
}

fun ByteArrayList.getFloat(index: Int): Float {
    return java.lang.Float.intBitsToFloat(getInt(index))
}

fun ByteArrayList.expand(addSize: Int) {
    ensureCapacity(size + addSize)
    for (i in 0 until addSize)
        add(0)
}
