package org.plos_clan.kbpf.instruction

import org.plos_clan.kbpf.BpfRuntime

data class BpfInsn(val cmd: UByteArray) {

    companion object {
        const val BPF_LD: UByte = 0u
        const val BPF_LDX: UByte = 1u
        const val BPF_ST: UByte = 2u
        const val BPF_STX: UByte = 3u
        const val BPF_ALU: UByte = 4u
        const val BPF_JMP: UByte = 5u
        const val BPF_JMP32: UByte = 6u
        const val BPF_ALU64: UByte = 7u

        const val BPF_K: UByte = 0u // imm
        const val BPF_X: UByte = 0x8u // register
    }

    init {
        require(cmd.size >= 8) { "eBPF instruction must be 8 bytes" }
    }

    val code: UByte get() = cmd[0]
    val dstReg: UByte get() = cmd[1] and 0X0FU
    val srcReg: UByte get() = (cmd[1].toUInt() shr 4).toUByte()
    val offset: Short
        get() = ((cmd[3].toInt() shl 8) or (cmd[2].toInt() and 0xFF)).toShort()
    val imm: Int
        get() = (cmd[7].toInt() shl 24) or
                ((cmd[6].toInt() and 0xFF) shl 16) or
                ((cmd[5].toInt() and 0xFF) shl 8) or
                (cmd[4].toInt() and 0xFF)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as BpfInsn
        return cmd.contentEquals(other.cmd)
    }

    override fun hashCode(): Int {
        return cmd.contentHashCode()
    }

    fun insnClass(): UByte = code and 0x07u

    fun insnSrc(): UByte = code and 0x08u

    fun insnOpcode(): UByte = code and 0xF0u

    override fun toString(): String = "{opcode=$code, srcReg=$srcReg, dstReg=$dstReg, offset=$offset, imm=$imm}"
}

fun BpfRuntime.getSrcValue(cmd: BpfInsn): ULong =
    if (cmd.insnSrc() == BpfInsn.BPF_X) {
        this.register[cmd.srcReg]
    } else {
        cmd.imm.toLong().toULong()
    }

fun BpfRuntime.getNextCommand(): BpfInsn? {
    if (this.register.pc >= this.insnList.size) return null
    val insn = this.insnList[this.register.pc]
    this.register.pc++
    return insn
}

fun BpfInsn.isExit(): Boolean {
    return this.code == (BpfInsn.BPF_JMP or BPF_EXIT)
}
