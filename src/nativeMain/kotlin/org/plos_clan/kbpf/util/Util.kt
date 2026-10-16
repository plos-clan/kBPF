package org.plos_clan.kbpf.util

fun UByteArray.getUShort(index: Int): UShort {
    return ((this[index].toUInt()       ) or
            (this[index + 1].toUInt() shl 8)).toUShort()
}

fun UByteArray.getUInt(index: Int): UInt {
    return (this[index].toUInt()        ) or
            (this[index + 1].toUInt() shl  8) or
            (this[index + 2].toUInt() shl 16) or
            (this[index + 3].toUInt() shl 24)
}

fun UByteArray.getULong(index: Int): ULong {
    return (this[index].toULong()        ) or
            (this[index + 1].toULong() shl  8) or
            (this[index + 2].toULong() shl 16) or
            (this[index + 3].toULong() shl 24) or
            (this[index + 4].toULong() shl 32) or
            (this[index + 5].toULong() shl 40) or
            (this[index + 6].toULong() shl 48) or
            (this[index + 7].toULong() shl 56)
}