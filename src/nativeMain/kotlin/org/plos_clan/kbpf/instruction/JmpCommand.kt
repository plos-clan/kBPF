package org.plos_clan.kbpf.instruction

import org.plos_clan.kbpf.BpfRuntime

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

fun BpfRuntime.jmpDispatch(cmd: BpfInsn) {
    when (cmd.insnOpcode()) {
        BPF_JGT -> this.jgt(cmd)
    }
}

fun BpfRuntime.jgt(cmd: BpfInsn) {
    val source: ULong = if(cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.srcReg]
    }else cmd.imm.toULong()
    if(this.register[cmd.dstReg] > source) this.register.pc += cmd.offset
}
