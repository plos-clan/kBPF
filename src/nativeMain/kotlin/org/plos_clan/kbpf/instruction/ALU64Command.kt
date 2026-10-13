package org.plos_clan.kbpf.instruction

import org.plos_clan.kbpf.BpfRuntime

const val BPF_ADD: UByte = 0x00u
const val BPF_SUB: UByte = 0x10u
const val BPF_MUL: UByte = 0x20u
const val BPF_DIV: UByte = 0x30u
const val BPF_OR:   UByte = 0x40u
const val BPF_AND:  UByte = 0x50u
const val BPF_LSH:  UByte = 0x60u
const val BPF_RSH:  UByte = 0x70u
const val BPF_NEG:  UByte = 0x80u
const val BPF_MOD:  UByte = 0x90u
const val BPF_XOR:  UByte = 0xa0u
const val BPF_MOV:  UByte = 0xb0u
const val BPF_ARSH: UByte = 0xc0u
const val BPF_END:  UByte = 0xd0u

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
        else -> error("unreachable")
    }

fun BpfRuntime.add(cmd: BpfInsn) =
    if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.dstReg] += this.register[cmd.srcReg]
    } else {
        this.register[cmd.dstReg] += cmd.imm.toULong()
    }

fun BpfRuntime.sub(cmd: BpfInsn) =
    if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.dstReg] -= this.register[cmd.srcReg]
    } else {
        this.register[cmd.dstReg] -= cmd.imm.toULong()
    }

fun BpfRuntime.mul(cmd: BpfInsn) =
    if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.dstReg] *= this.register[cmd.srcReg]
    } else {
        this.register[cmd.dstReg] *= cmd.imm.toULong()
    }

fun BpfRuntime.div(cmd: BpfInsn) {
    val src = if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.srcReg]
    } else {
        cmd.imm.toLong().toULong()
    }
    if (src == 0uL) {
        this.register[cmd.dstReg] = 0uL
    } else {
        this.register[cmd.dstReg] /= src
    }
}

fun BpfRuntime.mod(cmd: BpfInsn) {
    val src = if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.srcReg]
    } else {
        cmd.imm.toLong().toULong()
    }
    if (src != 0uL) {
        this.register[cmd.dstReg] %= src
    }
}

fun BpfRuntime.or(cmd: BpfInsn) {
    if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.dstReg] = this.register[cmd.dstReg] or this.register[cmd.srcReg]
    } else {
        this.register[cmd.dstReg] = this.register[cmd.dstReg] or cmd.imm.toLong().toULong()
    }
}

fun BpfRuntime.and(cmd: BpfInsn) {
    if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.dstReg] = this.register[cmd.dstReg] and this.register[cmd.srcReg]
    } else {
        this.register[cmd.dstReg] = this.register[cmd.dstReg] and cmd.imm.toLong().toULong()
    }
}

fun BpfRuntime.xor(cmd: BpfInsn) {
    if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.dstReg] = this.register[cmd.dstReg] xor this.register[cmd.srcReg]
    } else {
        this.register[cmd.dstReg] = this.register[cmd.dstReg] xor cmd.imm.toLong().toULong()
    }
}

fun BpfRuntime.lsh(cmd: BpfInsn) {
    val shift = if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.srcReg]
    } else {
        cmd.imm.toLong().toULong()
    }
    this.register[cmd.dstReg] = this.register[cmd.dstReg] shl (shift and 0x3Fu).toInt()
}

fun BpfRuntime.rsh(cmd: BpfInsn) {
    val shift = if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.srcReg]
    } else {
        cmd.imm.toLong().toULong()
    }
    this.register[cmd.dstReg] = this.register[cmd.dstReg] shr (shift and 0x3Fu).toInt()
}

fun BpfRuntime.arsh(cmd: BpfInsn) {
    val shift = if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.srcReg]
    } else {
        cmd.imm.toLong().toULong()
    }
    val signed = this.register[cmd.dstReg].toLong()
    this.register[cmd.dstReg] = (signed shr (shift and 0x3Fu).toInt()).toULong()
}

fun BpfRuntime.neg(cmd: BpfInsn) {
    this.register[cmd.dstReg] = (0L - this.register[cmd.dstReg].toLong()).toULong()
}

fun BpfRuntime.end(cmd: BpfInsn) {
    val dst = cmd.dstReg
    val width = cmd.imm
    val toBe = (cmd.code and 0x08u) != 0u.toUByte()   // true = BPF_TO_BE

    val value = this.register[dst]

    this.register[dst] = when (width) {
        16 -> {
            val v = value and 0xFFFFuL
            val swapped = ((v and 0xFFuL) shl 8) or ((v and 0xFF00uL) shr 8)
            if (toBe) swapped else v
        }
        32 -> {
            val v = value and 0xFFFFFFFFuL
            val swapped = ((v and 0xFFuL) shl 24) or
                    ((v and 0xFF00uL) shl 8) or
                    ((v and 0xFF0000uL) shr 8) or
                    ((v and 0xFF000000uL) shr 24)
            if (toBe) swapped else v
        }
        64 -> {
            val v = value
            val swapped =
                ((v and 0xFFuL) shl 56) or
                        ((v and 0xFF00uL) shl 40) or
                        ((v and 0xFF0000uL) shl 24) or
                        ((v and 0xFF000000uL) shl 8) or
                        ((v and 0xFF00000000uL) shr 8) or
                        ((v and 0xFF0000000000uL) shr 24) or
                        ((v and 0xFF000000000000uL) shr 40) or
                        ((v and 0xFF00000000000000uL) shr 56)
            if (toBe) swapped else v
        }
        else -> error("BPF_END only supports imm = 16 / 32 / 64, got $width")
    }
}

fun BpfRuntime.mov64(cmd: BpfInsn) =
    if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.dstReg] = this.register[cmd.srcReg]
    } else {
        this.register[cmd.dstReg] = cmd.imm.toULong()
    }
