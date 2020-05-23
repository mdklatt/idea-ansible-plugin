package software.mdklatt.idea.ansible.test.run

import org.jdom.Element
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import software.mdklatt.idea.ansible.run.GalaxyRunSettings


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
        options = listOf("one", " two  \"three\"")
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
            assertEquals(emptyList(), options)
            assertEquals("", workDir)
        }
    }

    /**
     * Test round-trip write/read with a JDOM Element.
     */
    @Test
    fun testJdomElementC() {
        val element = Element("configuration")
        settings.write(element)
        GalaxyRunSettings(element).apply {
            assertEquals(requirements, settings.requirements)
            assertEquals(rolesDir, settings.rolesDir)
            assertEquals(deps, settings.deps)
            assertEquals(force, settings.force)
            assertEquals(command, settings.command)
            assertEquals(options, settings.options)
        }
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
}
