package org.plos_clan.kbpf

import org.plos_clan.kbpf.instruction.BPF_B
import org.plos_clan.kbpf.instruction.BPF_DW
import org.plos_clan.kbpf.instruction.BPF_H
import org.plos_clan.kbpf.instruction.BPF_W
import org.plos_clan.kbpf.instruction.BpfInsn
import org.plos_clan.kbpf.util.BpfException
import org.plos_clan.kbpf.util.getUInt
import org.plos_clan.kbpf.util.getULong
import org.plos_clan.kbpf.util.getUShort

const val CONTEXT_AREA: ULong = 0x1000UL
const val CONTEXT_AREA_LENGTH: ULong = 0x1000UL

const val CALLSTACK_AREA: ULong = 0x2000UL
const val CALLSTACK_AREA_LENGTH: ULong = 4096UL

const val MAP_AREA: ULong = 0x3000UL

sealed class AccessSize {
    object BYTE : AccessSize() {
        override fun load(memory: UByteArray, offset: Int): ULong = memory[offset].toULong()
        override fun store(memory: UByteArray, offset: Int, value: ULong) {
            memory[offset] = (value and 0xffUL).toUByte()
        }
    }

    object HALF : AccessSize() {
        override fun load(memory: UByteArray, offset: Int): ULong = memory.getUShort(offset).toULong()
        override fun store(memory: UByteArray, offset: Int, value: ULong) {
            memory[offset] = (value and 0xffUL).toUByte()
            memory[offset + 1] = ((value shr 8) and 0xffUL).toUByte()
        }
    }

    object WORD : AccessSize() {
        override fun load(memory: UByteArray, offset: Int): ULong = memory.getUInt(offset).toULong()
        override fun store(memory: UByteArray, offset: Int, value: ULong) {
            for (i in 0 until 4) memory[offset + i] = ((value shr (i * 8)) and 0xffUL).toUByte()
        }
    }

    object DWORD : AccessSize() {
        override fun load(memory: UByteArray, offset: Int): ULong = memory.getULong(offset)
        override fun store(memory: UByteArray, offset: Int, value: ULong) {
            for (i in 0 until 8) memory[offset + i] = ((value shr (i * 8)) and 0xffUL).toUByte()
        }
    }

    abstract fun load(memory: UByteArray, offset: Int): ULong
    abstract fun store(memory: UByteArray, offset: Int, value: ULong)
}

fun BpfInsn.accessSize(): AccessSize =
    when (insnSize()) {
        BPF_W -> AccessSize.WORD
        BPF_H -> AccessSize.HALF
        BPF_B -> AccessSize.BYTE
        BPF_DW -> AccessSize.DWORD
        else -> error("Invalid BPF size")
    }

interface MemMapper {
    fun address(): ULong
    fun length(): ULong
    fun close()
}

private class MemoryMapperImpl(
    val data: UByteArray,
    val size: ULong,
    val address: ULong,
    val closed: () -> Unit
) : MemMapper {
    override fun address(): ULong = address

    override fun length(): ULong = size

    override fun close() = closed()
}

class MemoryArea(val runtime: BpfRuntime) {

    private val mappers = mutableListOf<MemoryMapperImpl>() // base_address | mapper
    private var nextMapperAddr = MAP_AREA

    // 'data' 不能是副本
    fun registerMapper(
        data: UByteArray,
        size: ULong = data.size.toULong(),
        closed: () -> Unit
    ): MemMapper {
        if (data.isEmpty()) throw BpfException("MemoryArea data is empty", runtime)

        val reserved = ((size + 7UL) and 7UL.inv()) + 8UL
        if (reserved > ULong.MAX_VALUE - nextMapperAddr) {
            throw BpfException("Virtual address space exhausted", runtime)
        }
        val address = nextMapperAddr
        nextMapperAddr += reserved

        val mapper = MemoryMapperImpl(data, size, address, closed)
        mappers.add(mapper)
        return mapper
    }

    fun deleteMapper(mapper: MemMapper) {
        mapper.close()
        mappers.remove(mapper)
    }

    fun store(address: ULong, width: AccessSize, data: ULong) = when (address) {
        // in CONTEXT_AREA until CALLSTACK_AREA -> TODO()
        in CALLSTACK_AREA until MAP_AREA -> storeStackAddress(address, width, data)
        in MAP_AREA until ULong.MAX_VALUE -> storeMapAddress(address, width, data)
        else -> throw BpfException("Invalid Memory area {store=$address}", runtime)
    }

    fun load(address: ULong, width: AccessSize): ULong = when (address) {
        //  in CONTEXT_AREA until CALLSTACK_AREA -> TODO()
        in CALLSTACK_AREA until MAP_AREA -> loadStackAddress(address, width)
        in MAP_AREA until ULong.MAX_VALUE -> loadMapAddress(address, width)
        else -> throw BpfException("Invalid Memory area {load=$address.toString()}", runtime)
    }

    private fun loadMapAddress(address: ULong, width: AccessSize): ULong {
        for (mapper in mappers)
            if ((mapper.address() until (mapper.address() + mapper.length())).contains(address)) {
                return width.load(mapper.data, 0)
            }
        throw BpfException("Invalid Memory map area {load=$address}", runtime)
    }

    private fun storeMapAddress(address: ULong, width: AccessSize, data: ULong) {
        for (mapper in mappers)
            if ((mapper.address() until (mapper.address() + mapper.length())).contains(address)) {
                width.store(mapper.data, 0, data)
                return
            }
        throw BpfException("Invalid Memory map area {load=$address}", runtime)
    }

    private fun storeStackAddress(addr: ULong, width: AccessSize, data: ULong) {
        val (frame, offset) = resolveStackAddress(addr) ?: throw BpfException("Invalid stack area $addr", runtime)
        width.store(frame.stackMemory, offset, data)
    }

    private fun loadStackAddress(addr: ULong, width: AccessSize): ULong {
        val (frame, offset) = resolveStackAddress(addr) ?: throw BpfException("Invalid stack area $addr", runtime)
        return width.load(frame.stackMemory, offset)
    }

    private fun resolveStackAddress(addr: ULong): Pair<StackFrame, Int>? {
        for (frame in runtime.callStack.reversed()) {
            val base = frame.baseAddr
            val end = base + MAX_BPF_STACK

            if (addr in base until end) {
                val offsetInFrame = (addr - base).toInt()
                return frame to offsetInFrame
            }
        }
        return null
    }
}
