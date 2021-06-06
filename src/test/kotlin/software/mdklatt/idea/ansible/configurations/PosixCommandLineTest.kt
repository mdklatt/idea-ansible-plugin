package software.mdklatt.idea.ansible.configurations

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull


/**
 * Unit tests for the PosixCommandLine class.
 */
internal class PosixCommandLineTest {

    private val aruments = listOf("pos1", "pos2")

    private val options = mapOf(
        "on" to true,
        "off" to false,
        "null" to null,
        "blank" to "",
        "int" to 123,
        "str" to "abc"
    )

    /**
     * Test the join() method.
     */
    @Test
    fun testJoin() {
        assertEquals("", PosixCommandLine.join(emptyList()))
        val argv = listOf("one", " two  \"three\"")
        val args = "one \" two  \\\"three\\\"\""
        assertEquals(args, PosixCommandLine.join(argv))
    }

    /**
     * Test the split() method.
     */
    @Test
    fun testSplit() {
        assertEquals(emptyList(), PosixCommandLine.split(""))
        val args = "one\t\n\r \" two  \\\"three\\\"\""
        val argv = listOf("one", " two  \"three\"")
        assertEquals(argv, PosixCommandLine.split(args))
    }

    /**
     * Test the constructor.
     */
    @Test
    fun testCtor() {
        val command = PosixCommandLine("test", aruments, options)
        assertEquals("test --on --blank \"\" --int 123 --str abc pos1 pos2", command.commandLineString)
    }

    /**
     * Test the addOptions() method.
     */
    @Test
    fun testAddOptions() {
        val command = PosixCommandLine("test")
        assertNotNull(command.addOptions(options))
        assertEquals("test --on --blank \"\" --int 123 --str abc", command.commandLineString)
    }

    /**
     * Test the createProcess() method.
     */
    @Test
    fun testCreateProcess() {
        val command = PosixCommandLine("echo", listOf("TEST"))
        command.createProcess().apply {
            assertEquals(0, waitFor())
            assertEquals("TEST\n", String(inputStream.readBytes()))
        }
    }

    /**
     * Test the withInput() method with text.
     */
    @Test
    fun testWithInputText() {
        val command = PosixCommandLine("cat")
        command.withInput("TEST")
        command.createProcess().apply {
            // The Process instance's inputStream is actually STDOUT from the
            // external command's point of view.
            assertEquals(0, waitFor())
            assertEquals("TEST", String(inputStream.readBytes()))
        }
    }

    /**
     * Test the withInput() method with data.
     */
    @Test
    fun testWithInputData() {
        val command = PosixCommandLine("cat")
        command.withInput(charArrayOf('T', 'E', 'S', 'T'))
        command.createProcess().apply {
            // The Process instance's inputStream is actually STDOUT from the
            // external command's point of view.
            assertEquals(0, waitFor())
            assertEquals("TEST", String(inputStream.readBytes()))
        }
    }

    /**
     * Test the withInput() method with a file.
     */
    @Test
    fun testWithInputFile() {
        val command = PosixCommandLine("cat")
        createTempFile().let {
            it.deleteOnExit()
            it.writeText("TEST")
            command.withInput(it)
        }
        command.createProcess().apply {
            assertEquals(0, waitFor())
            assertEquals("TEST", String(inputStream.readBytes()))
        }
    }
}
