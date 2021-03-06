package software.mdklatt.idea.ansible.test.run

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import software.mdklatt.idea.ansible.run.PosixCommandLine
import kotlin.test.assertNotNull


/**
 * Unit tests for the PosixCommandLine class.
 */
class PosixCommandLineTest {

    private val aruments = listOf("pos1", "pos2")

    private val options = mapOf(
        "on" to true,
        "off" to false,
        "null" to null,
        "int" to 123,
        "str" to "abc"
    )

    /**
     * Test the constructor.
     */
    @Test
    fun testCtor() {
        val command = PosixCommandLine("test", aruments, options)
        assertEquals("test --on --int 123 --str abc pos1 pos2", command.commandLineString)
    }

    /**
     * Test the addOptions() method.
     */
    @Test
    fun testAddOptions() {
        val command = PosixCommandLine("test")
        assertNotNull(command.addOptions(options))
        assertEquals("test --on --int 123 --str abc", command.commandLineString)
    }

    /**
     * Test the withInput() method with a text value.
     */
    @Test
    fun testWithInputText() {
        // Just a basic smoke test.
        val command = PosixCommandLine("cmd")
        assertNotNull(command.withInput("input"))
    }
}
