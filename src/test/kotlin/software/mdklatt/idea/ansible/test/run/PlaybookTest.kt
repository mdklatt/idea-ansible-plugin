package software.mdklatt.idea.ansible.test.run

import org.jdom.Element
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import software.mdklatt.idea.ansible.run.PlaybookRunSettings


/**
 * Unit tests for the PlaybookRunSettings class.
 */
class PlaybookRunSettingsTest {

    private var settings = PlaybookRunSettings().apply {
        playbooks = listOf("playbook.yml")
        inventory = listOf("hosts.yml")
        variables= listOf("key1=val1", "key2=vale")
        tags = listOf("abc", "xyz")
        options = listOf("one", " two  \"three\"")
    }

    /**
     * Test the primary constructor.
     */
    @Test
    fun testCtor() {
        PlaybookRunSettings().apply {
            assertEquals(emptyList(), playbooks)
            assertEquals(emptyList(), inventory)
            assertEquals("", host)
            assertEquals(emptyList(), tags)
            assertEquals(emptyList(), variables)
            assertEquals(emptyList(), options)
            assertEquals("ansible-playbook", command)
        }
    }

    /**
     * Test round-trip write/read with a JDOM Element.
     */
    @Test
    fun testJdomElementC() {
        val element = Element("configuration")
        settings.write(element)
        PlaybookRunSettings(element).apply {
            assertEquals(playbooks, settings.playbooks)
            assertEquals(inventory, settings.inventory)
            assertEquals(host, settings.host)
            assertEquals(tags, settings.tags)
            assertEquals(variables, settings.variables)
            assertEquals(options, settings.options)
            assertEquals(command, settings.command)
        }
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
}
