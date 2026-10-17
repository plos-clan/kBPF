package org.plos_clan.kbpf.instruction

import org.plos_clan.kbpf.BpfRuntime

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
    else -> error("Unreachable code: ${cmd.insnOpcode().toHexString()}")
}

fun BpfRuntime.mov32(cmd: BpfInsn) {
    this.register[cmd.dstReg] = this.getSrcValue32(cmd).toULong()
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
    if (src == 0u) this.register[cmd.dstReg] = 0uL
    else {
        val target: UInt = this.register[cmd.dstReg].toUInt() / src
        this.register[cmd.dstReg] = target.toULong()
    }
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
    val target: UInt = this.register[cmd.dstReg].toUInt() shl (shift and 0x3Fu).toInt()
    this.register[cmd.dstReg] = target.toULong()
}

fun BpfRuntime.rsh32(cmd: BpfInsn) {
    val shift = this.getSrcValue32(cmd)
    val target: UInt = this.register[cmd.dstReg].toUInt() shr (shift and 0x3Fu).toInt()
    this.register[cmd.dstReg] = target.toULong()
}
