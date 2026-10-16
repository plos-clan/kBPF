package org.plos_clan.kbpf.verifier.cmd

import org.plos_clan.kbpf.instruction.BpfLongInsn
import org.plos_clan.kbpf.instruction.BpfShortInsn
import org.plos_clan.kbpf.util.VerifierException

fun checkCmdElement(msg: String, target: UByte, vararg args: UByte) {
    if (args.isEmpty()) return
    for (arg in args)
        if (arg != target) throw VerifierException(msg)
}

fun longCmdCheck(cmd: BpfLongInsn): BpfLongInsn {
    val secCmd = BpfShortInsn(cmd.secCmd)
    checkCmdElement("eBPF long cmd must zero", 0u, secCmd.code, secCmd.srcReg, secCmd.dstReg)
    return cmd
}

fun shortCmdCheck(cmd: BpfShortInsn): BpfShortInsn {

    return cmd
}
