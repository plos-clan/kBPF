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

fun BpfRuntime.div(cmd: BpfInsn) =
    if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.dstReg] /= this.register[cmd.srcReg]
    } else {
        this.register[cmd.dstReg] /= cmd.imm.toULong()
    }

fun BpfRuntime.mov64(cmd: BpfInsn) =
    if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.dstReg] = this.register[cmd.srcReg]
    } else {
        this.register[cmd.dstReg] = cmd.imm.toULong()
    }
