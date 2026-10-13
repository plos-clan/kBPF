package org.plos_clan.kbpf.util

import kotlin.RuntimeException

class VerifierException(message: String) : RuntimeException(message)

class BpfException(message: String) : RuntimeException(message)