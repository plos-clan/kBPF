package org.plos_clan.kbpf.instruction

import org.plos_clan.kbpf.BpfRuntime
import org.plos_clan.kbpf.accessSize
import org.plos_clan.kbpf.util.BpfException

const val BPF_W: UByte = 0x00u
const val BPF_H: UByte = 0x08u
const val BPF_B: UByte = 0x10u
const val BPF_DW: UByte = 0x18u

const val BPF_MEM: UByte = 0x60u  // unsigned
const val BPF_MEMSX: UByte = 0x80u  // signed (cpuv4)

const val BPF_LD: UByte = 0x00u
const val BPF_LDX: UByte = 0x01u

const val BPF_IMM: UByte = 0x00u
const val BPF_ABS: UByte = 0x20u
const val BPF_IND: UByte = 0x40u

const val BPF_LD_IMM64: UByte = 0x18U

fun addSigned(base: ULong, offset: Short): ULong =
    if (offset >= 0) {
        base + offset.toULong()
    } else {
        base - (-offset.toLong()).toULong()
    }

fun signExtend(value: ULong, width: Int): ULong =
    when (width) {
        1 -> value.toByte().toLong().toULong()
        2 -> value.toShort().toLong().toULong()
        4 -> value.toInt().toLong().toULong()
        8 -> value
        else -> error("Invalid sign extension width")
    }

fun BpfRuntime.ldDispatch(cmd: BpfInsn) = when (cmd.insnMode()) {
    BPF_IMM -> this.ldimm64(cmd as BpfLongInsn)
    else -> error("unreachable")
}

fun BpfRuntime.stDispatch(cmd: BpfInsn) = when (cmd.insnMode()) {
    BPF_MEM -> stMem(cmd)
    else -> error("unreachable")
}

fun BpfRuntime.ldxDispatch(cmd: BpfInsn) = when (cmd.insnMode()) {
    BPF_MEM -> ldxMem(cmd)
    else -> error("unreachable")
}

fun BpfRuntime.stxDispatch(cmd: BpfInsn) = when (cmd.insnMode()) {
    BPF_MEM -> stxMem(cmd)
    else -> error("unreachable")
}

fun BpfRuntime.stMem(cmd: BpfInsn) {
    val size = cmd.accessSize()
    val address = addSigned(register[cmd.dstReg], cmd.offset)
    memory.store(address, size, cmd.imm.toULong())
}

fun BpfRuntime.stxMem(cmd: BpfInsn) {
    val size = cmd.accessSize()
    val addr = addSigned(register[cmd.dstReg], cmd.offset)
    memory.store(addr, size, register[cmd.srcReg])
}

fun BpfRuntime.ldxMem(cmd: BpfInsn) {
    val size = cmd.accessSize()
    val addr = addSigned(register[cmd.srcReg], cmd.offset)
    register[cmd.dstReg] = memory.load(addr, size)
}

fun BpfRuntime.ldimm64(cmd: BpfLongInsn) {
    this.getNextCommand() // pop empty cmd
    if (cmd.srcReg != 0.toUByte()) {
        throw BpfException("Unsupported LD IMM64 pseudo source")
    }
    register[cmd.dstReg] = cmd.getFullImmediate()
}