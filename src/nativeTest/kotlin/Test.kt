import fixtures.bpfProgramBytes
import org.plos_clan.kbpf.BpfRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BpfTest {
    @Test
    fun test() {
        val source = bpfProgramBytes().toUByteArray()
        assertTrue(source.isNotEmpty())

        val runtime = BpfRuntime(source)
        assertEquals(2UL, runtime.run())
    }
}