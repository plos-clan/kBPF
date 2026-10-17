package org.plos_clan.kbpf

import org.plos_clan.kbpf.instruction.*
import org.plos_clan.kbpf.lib.HelperLibrary
import org.plos_clan.kbpf.util.BpfException
import org.plos_clan.kbpf.util.VerifierException
import org.plos_clan.kbpf.verifier.Verifier

const val MAX_BPF_STACK = 512UL
const val MAX_CALL_FRAMES = 8

class Registers {
    companion object {
        const val R0: UByte = 0u
        const val R1: UByte = 1u
        const val R2: UByte = 2u
        const val R3: UByte = 3u
        const val R4: UByte = 4u
        const val R5: UByte = 5u
        const val R6: UByte = 6u
        const val R7: UByte = 7u
        const val R8: UByte = 8u
        const val R9: UByte = 9u
        const val R10: UByte = 10u
    }

    val registers = ULongArray(11)

    var pc: Int = 0 // insnList index

    operator fun get(index: UByte) = registers[index.toInt()]
    operator fun set(index: UByte, data: ULong) {
        registers[index.toInt()] = data
    }
}

data class StackFrame(
    val registers: ULongArray = ULongArray(5),
    val stackMemory: UByteArray = UByteArray(MAX_BPF_STACK.toInt()),
    val pc: Int,
    val type: Type = Type.USER,
    val baseAddr: ULong = 0u,
) {
    init {
        require(registers.size == 5)
    }

    fun loadU8(addr: ULong): UByte = stackMemory[addr.toInt()]
    fun loadU16(addr: ULong): UShort {
        val offset = addr.toInt()
        val b0 = stackMemory[offset].toUInt()
        val b1 = stackMemory[offset + 1].toUInt()
        return ((b1 shl 8) or b0).toUShort()
    }

    fun loadU32(addr: ULong): UInt {
        val offset = addr.toInt()
        var value = 0u
        for (i in 0 until 4) {
            value = value or (stackMemory[offset + i].toUInt() shl (i * 8))
        }
        return value
    }

    fun loadU64(addr: ULong): ULong {
        val offset = addr.toInt()
        var value = 0uL
        for (i in 0 until 8) {
            value = value or (stackMemory[offset + i].toULong() shl (i * 8))
        }
        return value
    }

    fun loadS8(addr: ULong): Byte = loadU8(addr).toByte()
    fun loadS16(addr: ULong): Short = loadU16(addr).toShort()
    fun loadS32(addr: ULong): Int = loadU32(addr).toInt()
    fun loadS64(addr: ULong): Long = loadU64(addr).toLong()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as StackFrame
        return registers.contentEquals(other.registers)
    }

    override fun hashCode(): Int {
        return registers.contentHashCode()
    }

    enum class Type {
        ROOT,
        USER,
    }
}

class BpfRuntime(
    val source: UByteArray
) {
    init {
        if (source.size % 8 != 0) throw VerifierException("eBPF program size must be multiple of 8")
    }

    val register = Registers()
    var status = Status.LOADED

    lateinit var insnList: List<BpfInsn>

    val callStack = ArrayDeque<StackFrame>()
    val memory = MemoryArea(this)

    val helper = HelperLibrary(this)

    fun topStackFrame(): StackFrame = callStack.last()

    fun createStackFrame(type: StackFrame.Type, pc: Int = 0): StackFrame {
        val baseAddr = callStack.size.toULong() * MAX_BPF_STACK + CALLSTACK_AREA
        val frame = StackFrame(pc = pc, type = type, baseAddr = baseAddr)
        callStack.addLast(frame)
        register[Registers.R10] = baseAddr + MAX_BPF_STACK
        return frame
    }

    fun run(): ULong {
        this.status = Status.CHECKING
        this.insnList = Verifier.checkSource(source)
        this.status = Status.RUNNING
        createStackFrame(type = StackFrame.Type.ROOT)
        while (true) {
            val cmd = this.getNextCommand() ?: throw BpfException("failed to run ebpf command: illegal end")
            if (cmd.isExit()) if (topStackFrame().type == StackFrame.Type.USER) {
                val frame = callStack.removeLast()
                register[Registers.R10] -= MAX_BPF_STACK
                this.register.pc = frame.pc
                frame.registers.copyInto(
                    destination = register.registers,
                    destinationOffset = 6,
                    startIndex = 0,
                    endIndex = 5
                )
                register[Registers.R10] = topStackFrame().baseAddr + MAX_BPF_STACK
                continue
            } else break
            when (cmd.insnClass()) {
                BpfInsn.BPF_ALU64 -> this.alu64Dispatch(cmd)
                BpfInsn.BPF_ALU -> this.aluDispatch(cmd)
                BpfInsn.BPF_JMP -> this.jmpDispatch(cmd)
                BpfInsn.BPF_JMP32 -> this.executeJmp32(cmd)
                BpfInsn.BPF_LD -> this.ldDispatch(cmd)
                BpfInsn.BPF_LDX -> this.ldxDispatch(cmd)
                BpfInsn.BPF_ST -> this.stDispatch(cmd)
                BpfInsn.BPF_STX -> this.stxDispatch(cmd)
                else -> error("unreachable: unknown class: ${cmd.insnClass()}")
            }
        }
        this.status = Status.EXITED
        return this.register[Registers.R0]
    }

    enum class Status {
        LOADED,
        CHECKING,
        RUNNING,
        EXITED,
    }
}