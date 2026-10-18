package org.plos_clan.kbpf.util

import org.plos_clan.kbpf.BpfRuntime
import org.plos_clan.kbpf.Registers
import kotlin.RuntimeException

class VerifierException(message: String) : RuntimeException(message)

class BpfException(message: String, val runtime: BpfRuntime) : Exception(message) {

    private fun getRegister(index: UByte): String = if (index == 0u.toUByte()) {
        runtime.register.pc.toHexString()
    } else {
        runtime.register[index].toHexString()
    }

    fun dumpRuntimeInfo(): String =
        buildString {
            append("BPF error: $message\n")
            append("call_stack_deep=${runtime.callStack.size}\n")
            append("pc=0x${getRegister(0u)} r10=0x${getRegister(Registers.R10)}\n")
            append("r0=0x${getRegister(Registers.R0)} r1=0x${getRegister(Registers.R1)}\n")
            append("r2=0x${getRegister(Registers.R2)} r3=0x${getRegister(Registers.R3)}\n")
            append("r4=0x${getRegister(Registers.R4)} r5=0x${getRegister(Registers.R5)}\n")
            append("r6=0x${getRegister(Registers.R6)} r7=0x${getRegister(Registers.R7)}\n")
            append("r8=0x${getRegister(Registers.R8)} r9=0x${getRegister(Registers.R9)}\n")

            append("id=${runtime.register[Registers.R6]}")
        }
}