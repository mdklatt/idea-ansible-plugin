/**
 * Unit tests for the Galaxy module.
 */
package software.mdklatt.idea.ansible.test.run

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jdom.Element
import org.junit.jupiter.api.Test
import software.mdklatt.idea.ansible.run.AnsibleConfigurationType
import software.mdklatt.idea.ansible.run.GalaxyConfigurationFactory
import software.mdklatt.idea.ansible.run.GalaxySettings


/**
 * Unit tests for the GalaxyConfigurationFactory class.
 */
class GalaxyConfigurationFactoryTest {

    private val factory = GalaxyConfigurationFactory(AnsibleConfigurationType())

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
 * Unit tests for the GalaxyRunSettings class.
 */
class GalaxySettingsTest {

    private var settings = GalaxySettings().apply {
        requirements = "reqs.yml"
        rolesDir = "roles/"
        deps = false
        force = true
        command = "ansbile-command"
        rawOpts = "one \"two\""
        workDir = "abc/"
    }

    /**
     * Test the primary constructor.
     */
    @Test
    fun testCtor() {
        GalaxySettings().apply {
            assertEquals("", requirements)
            assertEquals(true, deps)
            assertEquals(false, force)
            assertEquals("ansible-galaxy", command)
            assertEquals("", rawOpts)
            assertEquals("", workDir)
        }
    }

    /**
     * Test round-trip write/read with a JDOM Element.
     */
    @Test
    fun testPersistence() {
        val element = Element("configuration")
        settings.save(element)
        val newSettings = GalaxySettings()
        newSettings.load(element)
        newSettings.apply {
            assertEquals(requirements, settings.requirements)
            assertEquals(rolesDir, settings.rolesDir)
            assertEquals(deps, settings.deps)
            assertEquals(force, settings.force)
            assertEquals(command, settings.command)
            assertEquals(rawOpts, settings.rawOpts)
        }
    }

    /**
     * Test the command attribute.
     */
    @Test
    fun testCommand() {
        GalaxySettings().apply {
            command = ""
            assertEquals("ansible-galaxy", command)
            command = "abc"
            assertEquals("abc", command)
        }
    }
}
