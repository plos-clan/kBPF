package org.plos_clan.kbpf.verifier

import org.plos_clan.kbpf.instruction.BPF_LD_IMM64
import org.plos_clan.kbpf.instruction.BpfEmptyInsn
import org.plos_clan.kbpf.instruction.BpfInsn
import org.plos_clan.kbpf.instruction.BpfLongInsn
import org.plos_clan.kbpf.instruction.BpfShortInsn
import org.plos_clan.kbpf.util.VerifierException
import org.plos_clan.kbpf.verifier.cmd.longCmdCheck
import org.plos_clan.kbpf.verifier.cmd.shortCmdCheck

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

                instructions += longCmdCheck(BpfLongInsn(fstCmd, secCmd))
                BpfEmptyInsn
            }else {
                offset += 8
                shortCmdCheck(BpfShortInsn(fstCmd))
            }
            instructions += target
        }

        return instructions
    }
}