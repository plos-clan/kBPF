package org.plos_clan.kbpf.instruction

import org.plos_clan.kbpf.BpfRuntime
import org.plos_clan.kbpf.Registers
import org.plos_clan.kbpf.lib.callHelper
import org.plos_clan.kbpf.util.BpfException

const val BPF_JA: UByte = 0x00u
const val BPF_JEQ: UByte = 0x10u
const val BPF_JGT: UByte = 0x20u
const val BPF_JGE: UByte = 0x30u
const val BPF_JSET: UByte = 0x40u
const val BPF_JNE: UByte = 0x50u
const val BPF_JSGT: UByte = 0x60u
const val BPF_JSGE: UByte = 0x70u
const val BPF_CALL: UByte = 0x80u
const val BPF_EXIT: UByte = 0x90u
const val BPF_JLT:  UByte = 0xa0u
const val BPF_JLE:  UByte = 0xb0u
const val BPF_JSLT: UByte = 0xc0u
const val BPF_JSLE: UByte = 0xd0u

fun BpfRuntime.jmpDispatch(cmd: BpfInsn) {
    when (cmd.insnOpcode()) {
        BPF_JA -> this.ja(cmd)
        BPF_JGT -> this.jgt(cmd)
        BPF_JEQ -> this.jeq(cmd)
        BPF_JGE -> this.jge(cmd)
        BPF_JSET -> this.jset(cmd)
        BPF_JNE -> this.jne(cmd)
        BPF_JSGT -> this.jsgt(cmd)
        BPF_JSGE -> this.jsge(cmd)
        BPF_CALL -> this.call(cmd)
        BPF_JSLT -> this.jslt(cmd)
        BPF_JSLE -> this.jsle(cmd)
        BPF_JLE -> this.jle(cmd)
        BPF_JLT -> this.jlt(cmd)
        else -> error("unreachable")
    }
}

fun BpfRuntime.ja(cmd: BpfInsn) {
    this.register.pc += cmd.offset
}

fun BpfRuntime.jeq(cmd: BpfInsn) {
    val source: ULong = this.getSrcValue(cmd)
    if (this.register[cmd.dstReg] == source) this.register.pc += cmd.offset
}

fun BpfRuntime.jgt(cmd: BpfInsn) {
    val source: ULong = this.getSrcValue(cmd)
    if (this.register[cmd.dstReg] > source) this.register.pc += cmd.offset
}

fun BpfRuntime.jge(cmd: BpfInsn) {
    val source = getSrcValue(cmd)
    if (this.register[cmd.dstReg] >= source) this.register.pc += cmd.offset
}

fun BpfRuntime.jset(cmd: BpfInsn) {
    val source = getSrcValue(cmd)
    if ((this.register[cmd.dstReg] and source) != 0UL) this.register.pc += cmd.offset
}

fun BpfRuntime.jne(cmd: BpfInsn) {
    val source = getSrcValue(cmd)
    if (this.register[cmd.dstReg] != source) this.register.pc += cmd.offset
}

fun BpfRuntime.jsgt(cmd: BpfInsn) {
    val source = getSrcValue(cmd).toLong()
    if (this.register[cmd.dstReg].toLong() > source) this.register.pc += cmd.offset
}

fun BpfRuntime.jslt(cmd: BpfInsn) {
    val source = getSrcValue(cmd).toLong()
    if (this.register[cmd.dstReg].toLong() < source) this.register.pc += cmd.offset
}

fun BpfRuntime.jsge(cmd: BpfInsn) {
    val source = getSrcValue(cmd).toLong()
    if (this.register[cmd.dstReg].toLong() >= source) this.register.pc += cmd.offset
}

fun BpfRuntime.jsle(cmd: BpfInsn) {
    val source = getSrcValue(cmd).toLong()
    if (this.register[cmd.dstReg].toLong() <= source) this.register.pc += cmd.offset
}

fun BpfRuntime.jle(cmd: BpfInsn) {
    val source = getSrcValue(cmd)
    if (this.register[cmd.dstReg] <= source) this.register.pc += cmd.offset
}

fun BpfRuntime.jlt(cmd: BpfInsn) {
    val source = getSrcValue(cmd)
    if (this.register[cmd.dstReg] < source) this.register.pc += cmd.offset
}

fun BpfRuntime.call(cmd: BpfInsn) : Unit = when (cmd.srcReg.toInt()) {
    0, 2 -> {
        val id = cmd.imm
        val arg1 = register[Registers.R1]
        val arg2 = register[Registers.R2]
        val arg3 = register[Registers.R3]
        val arg4 = register[Registers.R4]
        val arg5 = register[Registers.R5]
        register[Registers.R0] = callHelper(id ,arg1, arg2, arg3, arg4, arg5)
    }
    1 -> {
        callStack.addLast(register.pc)
        register.pc += cmd.imm
    }
    else -> throw BpfException("Invalid CALL src_reg: ${cmd.srcReg}")
}
