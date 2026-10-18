package org.plos_clan.kbpf.instruction

import org.plos_clan.kbpf.BpfRuntime
import org.plos_clan.kbpf.util.BpfException

const val BPF_ADD: UByte = 0x00u
const val BPF_SUB: UByte = 0x10u
const val BPF_MUL: UByte = 0x20u
const val BPF_DIV: UByte = 0x30u
const val BPF_OR: UByte = 0x40u
const val BPF_AND: UByte = 0x50u
const val BPF_LSH: UByte = 0x60u
const val BPF_RSH: UByte = 0x70u
const val BPF_NEG: UByte = 0x80u
const val BPF_MOD: UByte = 0x90u
const val BPF_XOR: UByte = 0xa0u
const val BPF_MOV: UByte = 0xb0u
const val BPF_ARSH: UByte = 0xc0u
const val BPF_END: UByte = 0xd0u

fun BpfRuntime.alu64Dispatch(cmd: BpfInsn) =
    when (cmd.insnOpcode()) {
        BPF_MOV -> this.mov64(cmd)
        BPF_ADD -> this.add(cmd)
        BPF_SUB -> this.sub(cmd)
        BPF_MUL -> this.mul(cmd)
        BPF_DIV -> this.div(cmd)
        BPF_OR -> this.or(cmd)
        BPF_AND -> this.and(cmd)
        BPF_XOR -> this.xor(cmd)
        BPF_LSH -> this.lsh(cmd)
        BPF_RSH -> this.rsh(cmd)
        BPF_MOD -> this.mod(cmd)
        BPF_ARSH -> this.arsh(cmd)
        BPF_NEG -> this.neg(cmd)
        BPF_END -> this.end(cmd)
        else -> throw BpfException("Unreachable code: alu64 ${cmd.insnOpcode().toHexString()}", this)
    }

fun BpfRuntime.add(cmd: BpfInsn) {
    val src = this.getSrcValue(cmd)
    this.register[cmd.dstReg] += src
}

fun BpfRuntime.sub(cmd: BpfInsn) {
    val src = this.getSrcValue(cmd)
    this.register[cmd.dstReg] -= src
}

fun BpfRuntime.mul(cmd: BpfInsn) {
    val src = this.getSrcValue(cmd)
    this.register[cmd.dstReg] *= src
}

fun BpfRuntime.div(cmd: BpfInsn) {
    val src = this.getSrcValue(cmd)
    this.register[cmd.dstReg] = when {
        src == 0uL ->  0uL
        cmd.offset == 0.toShort() -> this.register[cmd.dstReg] / src
        src.toLong() == -1L && this.register[cmd.dstReg].toLong() == Long.MIN_VALUE -> Long.MIN_VALUE.toULong()
        cmd.offset == 1.toShort() -> (this.register[cmd.dstReg].toLong() / src.toLong()).toULong()
        else -> throw BpfException("unreachable div", this)
    }
}

fun BpfRuntime.mod(cmd: BpfInsn) {
    val src = this.getSrcValue(cmd)
    val target = when {
        src == 0uL ->  0uL
        cmd.offset == 0.toShort() -> this.register[cmd.dstReg] % src
        src.toLong() == -1L && this.register[cmd.dstReg].toLong() == Long.MIN_VALUE -> 0UL
        cmd.offset == 1.toShort() -> (this.register[cmd.dstReg].toLong() % src.toLong()).toULong()
        else -> throw BpfException("unreachable mod", this)
    }
    this.register[cmd.dstReg] = target
}

fun BpfRuntime.or(cmd: BpfInsn) {
    val src = this.getSrcValue(cmd)
    this.register[cmd.dstReg] = this.register[cmd.dstReg] or src
}

fun BpfRuntime.and(cmd: BpfInsn) {
    val src = this.getSrcValue(cmd)
    this.register[cmd.dstReg] = this.register[cmd.dstReg] and src
}

fun BpfRuntime.xor(cmd: BpfInsn) {
    val src = this.getSrcValue(cmd)
    this.register[cmd.dstReg] = this.register[cmd.dstReg] xor src
}

fun BpfRuntime.lsh(cmd: BpfInsn) {
    val shift = this.getSrcValue(cmd)
    this.register[cmd.dstReg] = this.register[cmd.dstReg] shl (shift and 0x3Fu).toInt()
}

fun BpfRuntime.rsh(cmd: BpfInsn) {
    val shift = this.getSrcValue(cmd)
    this.register[cmd.dstReg] = this.register[cmd.dstReg] shr (shift and 0x3Fu).toInt()
}

fun BpfRuntime.arsh(cmd: BpfInsn) {
    val shift = this.getSrcValue(cmd)
    val signed = this.register[cmd.dstReg].toLong()
    this.register[cmd.dstReg] = (signed shr (shift and 0x3Fu).toInt()).toULong()
}

fun BpfRuntime.neg(cmd: BpfInsn) {
    this.register[cmd.dstReg] = (0L - this.register[cmd.dstReg].toLong()).toULong()
}

fun BpfRuntime.end(cmd: BpfInsn) {
    val width = cmd.imm
    if (width != 16 && width != 32 && width != 64) {
        throw BpfException("BPF_END only supports widths 16 / 32 / 64, got $width", this)
    }

    val swap = when (cmd.insnClass()) {
        BpfInsn.BPF_ALU -> cmd.insnSrc() == BpfInsn.BPF_X
        BpfInsn.BPF_ALU64 -> {
            if (cmd.insnSrc() != BpfInsn.BPF_K) {
                throw BpfException("ALU64 BPF_END requires the source bit to be zero", this)
            }
            true
        }
        else -> throw BpfException("Invalid instruction class for BPF_END", this)
    }

    val value = register[cmd.dstReg]
    register[cmd.dstReg] = if (swap) {
        var remaining = value
        var result = 0UL
        repeat(width / 8) {
            result = (result shl 8) or (remaining and 0xFFUL)
            remaining = remaining shr 8
        }
        result
    } else {
        when (width) {
            16 -> value.toUShort().toULong()
            32 -> value.toUInt().toULong()
            else -> value
        }
    }
}

fun BpfRuntime.mov64(cmd: BpfInsn)  {
    if (cmd.offset.toInt() != 0 && cmd.insnSrc() != BpfInsn.BPF_X) {
        throw BpfException("MOVSX requires a register source", this)
    }

    val src = getSrcValue(cmd)
    register[cmd.dstReg] = when (cmd.offset.toInt()) {
        0 -> src
        8 -> src.toByte().toLong().toULong()
        16 -> src.toShort().toLong().toULong()
        32 -> src.toInt().toLong().toULong()
        else -> throw BpfException("Invalid MOV64 offset: ${cmd.offset}", this)
    }
}