package org.plos_clan.kbpf.verifier

import org.plos_clan.kbpf.instruction.BPF_LD_IMM64
import org.plos_clan.kbpf.instruction.BpfInsn
import org.plos_clan.kbpf.instruction.BpfLongInsn
import org.plos_clan.kbpf.instruction.BpfShortInsn
import org.plos_clan.kbpf.util.VerifierException

object Verifier {
    fun checkSource(source: UByteArray): List<BpfInsn> {
        val instructions = mutableListOf<BpfInsn>()
        var offset = 0

        while (offset < source.size) {
            if (offset + 8 > source.size)
                throw VerifierException("Invalid BPF bytecode: unexpected end of stream at offset $offset")
            val fstCmd = source.copyOfRange(offset, offset + 8)
            val opcode = fstCmd[0]
            val target = if(opcode == BPF_LD_IMM64) {
                if(offset + 16 > source.size)
                    throw VerifierException("Invalid BPF Long bytecode: unexpected end of stream at offset $offset")
                val secCmd = source.copyOfRange(offset + 8, offset + 16)
                offset += 16
                BpfLongInsn(fstCmd, secCmd)
            }else {
                offset += 8
                BpfShortInsn(fstCmd)
            }
            instructions += target
        }

        return instructions
    }
}