package org.plos_clan.kbpf

import org.plos_clan.kbpf.instruction.BpfInsn
import org.plos_clan.kbpf.instruction.alu64Dispatch
import org.plos_clan.kbpf.instruction.getNextCommand
import org.plos_clan.kbpf.instruction.isExit
import org.plos_clan.kbpf.instruction.jmpDispatch
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

    private val registers = ULongArray(11)

    var pc: Int = 0 // insnList index

    operator fun get(index: UByte) = registers[index.toInt()]
    operator fun set(index: UByte, data: ULong) {
        registers[index.toInt()] = data
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
    val insnList: List<BpfInsn> = (source.indices step 8).map { offset ->
        BpfInsn(source.copyOfRange(offset, offset + 8))

    }
    val callStack = ArrayDeque<Int>()

    fun run(): ULong {
        this.status = Status.CHECKING
        Verifier.checkSource(this.insnList)
        this.status = Status.RUNNING
        while (true) {
            val cmd = this.getNextCommand() ?: throw BpfException("failed to run ebpf command: illegal end")
            if (cmd.isExit()) if(callStack.isNotEmpty()) {
                this.register.pc = callStack.removeLast()
                continue
            }else break
            when (cmd.insnClass()) {
                BpfInsn.BPF_ALU64 -> this.alu64Dispatch(cmd)
                BpfInsn.BPF_JMP -> this.jmpDispatch(cmd)
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