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

class MemoryArea(val runtime: BpfRuntime) {

    fun store(address: ULong, width: AccessSize, data: ULong) = when (address) {
       // in CONTEXT_AREA until CALLSTACK_AREA -> TODO()
        in CALLSTACK_AREA until MAP_AREA -> storeStackAddress(address, width, data)
        else -> throw BpfException("Invalid Memory area {store=$address}")
    }

    fun load(address: ULong, width: AccessSize): ULong = when (address) {
      //  in CONTEXT_AREA until CALLSTACK_AREA -> TODO()
        in CALLSTACK_AREA until MAP_AREA -> loadStackAddress(address, width)
        else -> throw BpfException("Invalid Memory area {load=$address.toString()}")
    }

    fun storeStackAddress(addr: ULong, width: AccessSize, data: ULong) {
        val (frame, offset) = resolveStackAddress(addr) ?: throw BpfException("Invalid stack area $addr")
        width.store(frame.stackMemory, offset, data)
    }

    fun loadStackAddress(addr: ULong, width: AccessSize): ULong {
        val (frame, offset) = resolveStackAddress(addr) ?: throw BpfException("Invalid stack area $addr")
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
