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
const val BPF_ATOMIC: UByte = 0xC0u

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
    else -> throw BpfException("Unreachable code: ld ${cmd.insnOpcode().toHexString()}", this)
}

fun BpfRuntime.stDispatch(cmd: BpfInsn) = when (cmd.insnMode()) {
    BPF_MEM -> stMem(cmd)
    else -> throw BpfException("Unreachable code: st ${cmd.insnOpcode().toHexString()}", this)
}

fun BpfRuntime.ldxDispatch(cmd: BpfInsn) = when (cmd.insnMode()) {
    BPF_MEM -> ldxMem(cmd)
    BPF_MEMSX -> ldxMemSigned(cmd)
    else -> throw BpfException("Unreachable code: ldx ${cmd.insnOpcode().toHexString()}", this)
}

fun BpfRuntime.stxDispatch(cmd: BpfInsn) = when (cmd.insnMode()) {
    BPF_MEM -> stxMem(cmd)
    else -> throw BpfException("Unreachable code: stx ${cmd.insnOpcode().toHexString()}", this)
}

fun BpfRuntime.ldxMemSigned(cmd: BpfInsn) {
    val width = when (cmd.insnSize()) {
        BPF_B -> 1
        BPF_H -> 2
        BPF_W -> 4
        else -> throw BpfException("Invalid MEMSX size", this)
    }

    val address = addSigned(register[cmd.srcReg], cmd.offset)
    val value = memory.load(address, cmd.accessSize())
    register[cmd.dstReg] = signExtend(value, width)
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
        throw BpfException("Unsupported LD IMM64 pseudo source", this)
    }
    register[cmd.dstReg] = cmd.getFullImmediate()
}