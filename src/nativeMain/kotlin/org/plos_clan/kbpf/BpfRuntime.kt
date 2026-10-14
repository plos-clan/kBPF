package org.plos_clan.kbpf

import org.plos_clan.kbpf.instruction.*
import org.plos_clan.kbpf.lib.HelperLibrary
import org.plos_clan.kbpf.util.BpfException
import org.plos_clan.kbpf.util.VerifierException
import org.plos_clan.kbpf.verifier.Verifier

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
    val stackMemory: ByteArray = ByteArray(512),
    val pc: Int,
) {
    init {
        require(registers.size == 5)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as StackFrame
        return registers.contentEquals(other.registers)
    }

    override fun hashCode(): Int {
        return registers.contentHashCode()
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

    val helper = HelperLibrary(this)

    fun topStackFrame(): StackFrame = callStack.last()

    fun run(): ULong {
        this.status = Status.CHECKING
        this.insnList = Verifier.checkSource(source)
        this.status = Status.RUNNING
        while (true) {
            val cmd = this.getNextCommand() ?: throw BpfException("failed to run ebpf command: illegal end")
            if (cmd.isExit()) if(callStack.isNotEmpty()) {
                val frame = callStack.removeLast()
                this.register.pc = frame.pc
                frame.registers.copyInto(
                    destination = register.registers,
                    destinationOffset = 6,
                    startIndex = 0,
                    endIndex = 5
                )
                continue
            }else break
            when (cmd.insnClass()) {
                BpfInsn.BPF_ALU64 -> this.alu64Dispatch(cmd)
                BpfInsn.BPF_JMP -> this.jmpDispatch(cmd)
                BpfInsn.BPF_LD -> this.ldDispatch(cmd)
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