@file:OptIn(ExperimentalForeignApi::class)

import org.plos_clan.kbpf.BpfRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.cinterop.*
import platform.posix.*

class BpfTest {
    fun loadBin(name: String): UByteArray {
        val path = "src/nativeTest/resources/$name"
        val file = fopen(path, "rb") ?: error("Cannot open resource: $name")
        fseek(file, 0, SEEK_END)
        val size = ftell(file).toInt()
        fseek(file, 0, SEEK_SET)
        val buffer = UByteArray(size)
        buffer.usePinned { pinned ->
            fread(pinned.addressOf(0), 1.convert(), size.convert(), file)
        }
        fclose(file)
        return buffer
    }

    @Test
    fun test_long_jmp() {
        val source = loadBin("bpf_base1.bin")
        assertTrue(source.isNotEmpty())

        val runtime = BpfRuntime(source)
        assertEquals( 0x11111010UL, runtime.run())
    }

    @Test
    fun test_alu64_call() {
        val source = loadBin("bpf_base2.bin")
        assertTrue(source.isNotEmpty())

        val runtime = BpfRuntime(source)
        assertEquals(0UL,runtime.run())
    }

    @Test
    fun test_memory() {
        val source = loadBin("bpf_base3.bin")
        assertTrue(source.isNotEmpty())

        val runtime = BpfRuntime(source)
        assertEquals(2UL,runtime.run())
    }
}