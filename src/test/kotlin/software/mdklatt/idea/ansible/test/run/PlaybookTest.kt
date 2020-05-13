package software.mdklatt.idea.ansible.test.run

import org.jdom.Element
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import software.mdklatt.idea.ansible.run.PlaybookRunSettings


/**
 * Unit tests for the PlaybookRunSettings class.
 */
class PlaybookRunSettingsTest {

    private var settings = PlaybookRunSettings()

    init {
        settings.tags = listOf("abc", "xyz")
        settings.options = listOf("one", " two  \"three\"")
    }

    /**
     * Test the constructor.
     */
    @Test
    fun testCtor() {
        val settings = PlaybookRunSettings()
        assertEquals(emptyList(), settings.playbooks)
        assertEquals(emptyList(), settings.inventory)
        assertEquals("", settings.host)
        assertEquals(emptyList(), settings.tags)
        assertEquals(emptyList(), settings.variables)
        assertEquals(emptyList(), settings.options)
        assertEquals("ansible-playbook", settings.command)
    }

    /**
     * Test the command attribute.
     */
    @Test
    fun testCommand() {
        val settings = PlaybookRunSettings()
        settings.command = ""
        assertEquals("ansible-playbook", settings.command)
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
        val savedSettings = PlaybookRunSettings()
        savedSettings.read(elem)
        assertEquals(settings.playbooks, savedSettings.playbooks)
        assertEquals(settings.inventory, savedSettings.inventory)
        assertEquals(settings.host, savedSettings.host)
        assertEquals(settings.tags, savedSettings.tags)
        assertEquals(settings.variables, savedSettings.variables)
        assertEquals(settings.command, savedSettings.command)
        assertEquals(settings.options, savedSettings.options)
        assertEquals(settings.workdir, savedSettings.workdir)
    }
}
