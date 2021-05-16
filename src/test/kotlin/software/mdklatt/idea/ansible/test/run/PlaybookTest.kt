/**
 * Unit tests for the Playbook module.
 */
package software.mdklatt.idea.ansible.test.run

import org.jdom.Element
import org.junit.jupiter.api.Disabled
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import software.mdklatt.idea.ansible.run.AnsibleConfigurationType
import software.mdklatt.idea.ansible.run.PlaybookConfigurationFactory
import software.mdklatt.idea.ansible.run.PlaybookSettings
import kotlin.test.assertFalse
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
class PlaybookSettingsTest {

    private var settings = PlaybookSettings().apply {
        playbooks = listOf("playbook.yml")
        inventory = listOf("hosts.yml")
        host = "hostname"
        passwordPrompt = true
        variables= listOf("key1=val1", "key2=val2")
        tags = listOf("abc", "xyz")
        rawOpts = "one \"two\""
        command = "/path/to/ansible-playbook"
    }

    /**
     * Test the primary constructor.
     */
    @Test
    fun testCtor() {
        PlaybookSettings().apply {
            assertEquals(emptyList(), playbooks)
            assertEquals(emptyList(), inventory)
            assertEquals("", host)
            assertFalse(passwordPrompt)
            assertEquals(emptyList(), tags)
            assertEquals(emptyList(), variables)
            assertEquals("", rawOpts)
            assertEquals("ansible-playbook", command)
        }
    }

    /**
     * Test round-trip write/read of settings.
     */
    @Test
    @Disabled  // FIXME: NPE when using PasswordSafe. Does it need to mocked?
    fun testPersistence() {
        // TODO: Verify that "id" exists in element and is a valid UUID.
        // TODO: Test password storage.
        // uuid = "91bde0ce-f06b-43da-ab39-07bb5eb897a2"
        val element = Element("configuration")
        settings.save(element)
        settings = PlaybookSettings()
        val newSettings = PlaybookSettings()
        newSettings.load(element)
        newSettings.apply {
            assertEquals(playbooks, settings.playbooks)
            assertEquals(inventory, settings.inventory)
            assertEquals(host, settings.host)
            assertEquals(passwordPrompt, settings.passwordPrompt)
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
        PlaybookSettings().apply {
            command = ""
            assertEquals("ansible-playbook", command)
            command = "abc"
            assertEquals("abc", command)
        }
    }
}
