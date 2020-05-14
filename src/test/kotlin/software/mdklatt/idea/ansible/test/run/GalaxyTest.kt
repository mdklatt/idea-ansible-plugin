package software.mdklatt.idea.ansible.test.run.software.mdklatt.idea.ansible.test.run

import org.jdom.Element
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import software.mdklatt.idea.ansible.run.GalaxyRunSettings


/**
 * Unit tests for the GalaxyRunSettings class.
 */
class GalaxyRunSettingsTest {

    private var settings = GalaxyRunSettings()

    init {
        settings.options = listOf("one", " two  \"three\"")
    }

    /**
     * Test the constructor.
     */
    @Test
    fun testCtor() {
        val settings = GalaxyRunSettings()
        assertEquals("", settings.requirements)
        assertEquals(emptyList(), settings.options)
        assertEquals("ansible-galaxy", settings.command)
    }

    /**
     * Test the command attribute.
     */
    @Test
    fun testCommand() {
        val settings = GalaxyRunSettings()
        settings.command = ""
        assertEquals("ansible-galaxy", settings.command)
        settings.command = "abc"
        assertEquals("abc", settings.command)
    }

    /**
     * Test the read() and write() methods.
     */
    @Test
    fun testReadWrite() {
        // Verify round-tripping via write() and read().
        val elem = Element("elem")
        settings.write(elem)
        val savedSettings = GalaxyRunSettings()
        savedSettings.read(elem)
        assertEquals(settings.requirements, savedSettings.requirements)
        assertEquals(settings.command, savedSettings.command)
        assertEquals(settings.options, savedSettings.options)
        assertEquals(settings.workDir, savedSettings.workDir)
    }
}
