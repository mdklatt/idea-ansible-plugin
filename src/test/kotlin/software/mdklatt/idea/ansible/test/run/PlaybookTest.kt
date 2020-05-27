/**
 * Unit tests for the Playbook module.
 */
package software.mdklatt.idea.ansible.test.run

import org.jdom.Element
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import software.mdklatt.idea.ansible.run.AnsibleConfigurationType
import software.mdklatt.idea.ansible.run.PlaybookConfigurationFactory
import software.mdklatt.idea.ansible.run.PlaybookRunSettings
import kotlin.test.assertTrue


/**
 * Unit tests for the PlaybookConfigurationFactory class.
 */
class PlaybookConfigurationFactoryTest {

    private val factory = PlaybookConfigurationFactory(AnsibleConfigurationType())

    /**
     * Test the id property.
     */
    @Test
    fun testId() {
        assertTrue(factory.id.isNotBlank())
    }

    /**
     * Test the name property.
     */
    @Test
    fun testName() {
        assertTrue(factory.name.isNotBlank())
    }
}


/**
 * Unit tests for the PlaybookRunSettings class.
 */
class PlaybookRunSettingsTest {

    private var settings = PlaybookRunSettings().apply {
        playbooks = listOf("playbook.yml")
        inventory = listOf("hosts.yml")
        variables= listOf("key1=val1", "key2=val2")
        tags = listOf("abc", "xyz")
        rawOpts = "one \"two\""
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
            assertEquals("", rawOpts)
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
            assertEquals(rawOpts, settings.rawOpts)
            assertEquals(command, settings.command)
        }
    }

    /**
     * Test the command attribute.
     */
    @Test
    fun testCommand() {
        PlaybookRunSettings().apply {
            command = ""
            assertEquals("ansible-playbook", command)
            command = "abc"
            assertEquals("abc", command)
        }
    }
}
