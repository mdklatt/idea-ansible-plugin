/**
 * Unit tests for Galaxy.kt.
 */
package dev.mdklatt.idea.ansible.run

import com.intellij.execution.process.OSProcessHandler
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.mdklatt.idea.common.exec.CommandLine


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.


/**
 * Unit tests for the CommandLine class.
 */
internal class CommandLineTest : BasePlatformTestCase() {

    private lateinit var command: CommandLine

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        command = CommandLine("ansible", "--version")
    }

    /**
     * Test the setPythonVenv() extension method.
     */
    fun testSetPythonVenv() {
        command.setPythonVenv(".venv")
        OSProcessHandler(command).let {
            it.startNotify()
            it.waitFor()
            assertEquals(0, it.exitCode)
        }
    }

    /**
     * Test the withPythonVenv() extension method.
     */
    fun testWithPythonVenv() {
        assertTrue(command == command.withPythonVenv(".venv"))
        OSProcessHandler(command).let {
            it.startNotify()
            it.waitFor()
            assertEquals(0, it.exitCode)
        }
    }
}
