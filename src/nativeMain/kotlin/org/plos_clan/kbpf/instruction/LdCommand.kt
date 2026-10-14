package org.plos_clan.kbpf.instruction

import org.plos_clan.kbpf.BpfRuntime

const val BPF_LD: UByte = 0x00u
const val BPF_LDX: UByte = 0x01u

const val BPF_IMM: UByte = 0x00u
const val BPF_ABS: UByte = 0x20u
const val BPF_IND: UByte = 0x40u
const val BPF_MEM: UByte = 0x60u

const val BPF_B: UByte = 0x10u
const val BPF_H: UByte = 0x08u
const val BPF_W: UByte = 0x00u
const val BPF_DW: UByte = 0x18u

const val BPF_LD_IMM64: UByte = 0x18U

fun BpfRuntime.ldDispatch(cmd: BpfInsn) = when (cmd.insnOpcode()) {
    BPF_LD_IMM64 -> this.ldimm64(cmd as BpfLongInsn)
    else -> error("unreachable")
}

fun BpfRuntime.ldimm64(cmd: BpfLongInsn) {
    register[cmd.dstReg] = cmd.getFullImmediate()
}
