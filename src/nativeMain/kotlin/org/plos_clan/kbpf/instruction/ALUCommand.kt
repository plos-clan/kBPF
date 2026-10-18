package org.plos_clan.kbpf.instruction

import org.plos_clan.kbpf.BpfRuntime
import org.plos_clan.kbpf.util.BpfException

fun BpfRuntime.aluDispatch(cmd: BpfInsn) = when (cmd.insnOpcode()) {
    BPF_ADD -> add32(cmd)
    BPF_SUB -> sub32(cmd)
    BPF_MUL -> mul32(cmd)
    BPF_DIV -> div32(cmd)
    BPF_MOV -> mov32(cmd)
    BPF_AND -> and32(cmd)
    BPF_OR -> or32(cmd)
    BPF_XOR -> xor32(cmd)
    BPF_LSH -> lsh32(cmd)
    BPF_RSH -> rsh32(cmd)
    BPF_NEG -> neg32(cmd)
    BPF_MOD -> mod32(cmd)
    BPF_ARSH -> arsh32(cmd)
    BPF_END -> end(cmd)
    else -> throw BpfException("Unreachable code: alu ${cmd.insnOpcode().toHexString()}", this)
}

fun BpfRuntime.mov32(cmd: BpfInsn) {
    if (cmd.offset.toInt() != 0 && cmd.insnSrc() != BpfInsn.BPF_X) {
        throw BpfException("MOVSX requires a register source", this)
    }

    val src = getSrcValue32(cmd)
    val result: UInt = when (cmd.offset.toInt()) {
        0 -> src
        8 -> src.toByte().toInt().toUInt()
        16 -> src.toShort().toInt().toUInt()
        else -> throw BpfException("Invalid MOV32 offset: ${cmd.offset}", this)
    }

    register[cmd.dstReg] = result.toULong()
}

fun BpfRuntime.add32(cmd: BpfInsn) {
    val src = this.getSrcValue32(cmd)
    val target: UInt = this.register[cmd.dstReg].toUInt() + src
    this.register[cmd.dstReg] = target.toULong()
}

fun BpfRuntime.sub32(cmd: BpfInsn) {
    val src = this.getSrcValue32(cmd)
    val target: UInt = this.register[cmd.dstReg].toUInt() - src
    this.register[cmd.dstReg] = target.toULong()
}

fun BpfRuntime.mul32(cmd: BpfInsn) {
    val src = this.getSrcValue32(cmd)
    val target: UInt = this.register[cmd.dstReg].toUInt() * src
    this.register[cmd.dstReg] = target.toULong()
}

fun BpfRuntime.div32(cmd: BpfInsn) {
    val src = this.getSrcValue32(cmd)
    val target = when {
        src == 0u -> 0uL
        cmd.offset == 0.toShort() -> (this.register[cmd.dstReg].toUInt() / src).toULong()
        src.toInt() == -1 && this.register[cmd.dstReg].toInt() == Int.MAX_VALUE -> Int.MAX_VALUE.toULong()
        cmd.offset == 1.toShort() -> (this.register[cmd.dstReg].toInt() / src.toInt()).toUInt().toULong()
        else -> throw BpfException("unreachable div32", this)
    }
    this.register[cmd.dstReg] = target
}

fun BpfRuntime.or32(cmd: BpfInsn) {
    val src = this.getSrcValue32(cmd)
    val target: UInt = this.register[cmd.dstReg].toUInt() or src
    this.register[cmd.dstReg] = target.toULong()
}

fun BpfRuntime.and32(cmd: BpfInsn) {
    val src = this.getSrcValue32(cmd)
    val target: UInt = this.register[cmd.dstReg].toUInt() and src
    this.register[cmd.dstReg] = target.toULong()
}

fun BpfRuntime.xor32(cmd: BpfInsn) {
    val src = this.getSrcValue32(cmd)
    val target: UInt = this.register[cmd.dstReg].toUInt() xor src
    this.register[cmd.dstReg] = target.toULong()
}

fun BpfRuntime.lsh32(cmd: BpfInsn) {
    val shift = this.getSrcValue32(cmd)
    val target: UInt = this.register[cmd.dstReg].toUInt() shl (shift and 0x1Fu).toInt()
    this.register[cmd.dstReg] = target.toULong()
}

fun BpfRuntime.rsh32(cmd: BpfInsn) {
    val shift = this.getSrcValue32(cmd)
    val target: UInt = this.register[cmd.dstReg].toUInt() shr (shift and 0x1Fu).toInt()
    this.register[cmd.dstReg] = target.toULong()
}

fun BpfRuntime.neg32(cmd: BpfInsn) {
    this.register[cmd.dstReg] = (0 - this.register[cmd.dstReg].toInt()).toUInt().toULong()
}

fun BpfRuntime.mod32(cmd: BpfInsn) {
    val src = this.getSrcValue32(cmd)
    val target = when {
        src == 0u -> 0uL
        cmd.offset == 0.toShort() -> (this.register[cmd.dstReg].toUInt() % src).toULong()
        src.toInt() == -1 && this.register[cmd.dstReg].toInt() == Int.MAX_VALUE -> 0uL
        cmd.offset == 1.toShort() -> (this.register[cmd.dstReg].toInt() % src.toInt()).toUInt().toULong()
        else -> throw BpfException("unreachable mod32", this)
    }
    this.register[cmd.dstReg] = target
}

fun BpfRuntime.arsh32(cmd: BpfInsn) {
    val shift = (getSrcValue32(cmd) and 0x1Fu).toInt()
    val result = register[cmd.dstReg].toInt() shr shift
    register[cmd.dstReg] = result.toUInt().toULong()
}
