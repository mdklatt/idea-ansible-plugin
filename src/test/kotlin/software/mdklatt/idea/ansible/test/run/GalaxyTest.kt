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
import software.mdklatt.idea.ansible.run.GalaxyRunSettings


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
class GalaxyRunSettingsTest {

    private var settings = GalaxyRunSettings().apply {
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
        GalaxyRunSettings().apply {
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
    fun testJdomElement() {
        val element = Element("configuration")
        settings.write(element)
        GalaxyRunSettings(element).apply {
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
        GalaxyRunSettings().apply {
            command = ""
            assertEquals("ansible-galaxy", command)
            command = "abc"
            assertEquals("abc", command)
        }
    }
}
