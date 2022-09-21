/**
 * Unit tests for Galaxy.kt.
 */
package dev.mdklatt.idea.ansible.run

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jdom.Element


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.


/**
 * Unit tests for the GalaxyConfigurationFactory class.
 */
internal class GalaxyConfigurationFactoryTest : BasePlatformTestCase() {

    private lateinit var factory: GalaxyConfigurationFactory

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        factory = GalaxyConfigurationFactory(AnsibleConfigurationType())
    }

    /**
     * Test the `id` property.
     */
    fun testId() {
        assertTrue(factory.id.isNotBlank())
    }

    /**
     * Test the `name` property.
     */
    fun testName() {
        assertTrue(factory.name.isNotBlank())
    }

    /**
     * Test the testCreateTemplateConfiguration() method.
     */
    fun testCreateTemplateConfiguration() {
        // Just a smoke test to ensure that the expected RunConfiguration type
        // is returned.
        factory.createTemplateConfiguration(project).let {
            assertTrue(it.command.isNotBlank())
        }
    }
}


/**
 * Unit tests for the GalaxyRunConfiguration class.
 */
internal class GalaxyRunConfigurationTest : BasePlatformTestCase() {

    private lateinit var factory: GalaxyConfigurationFactory
    private lateinit var config: GalaxyRunConfiguration

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        factory = GalaxyConfigurationFactory(AnsibleConfigurationType())
        config = GalaxyRunConfiguration(project, factory, "Ansible Galaxy Test")
    }

    /**
     * Test the primary constructor.
     */
    fun testConstructor() {
        config.let {
            assertTrue(it.uid.isNotBlank())
            assertEquals("", it.requirements)
            assertTrue(it.deps)
            assertEquals("", it.rolesDir)
            assertFalse(it.force)
            assertEquals("ansible-galaxy", it.command)
            assertEquals("", it.rawOpts)
            assertEquals("", it.workDir)
        }
    }

    /**
     * Test round-trip write/read of settings.
     */
    fun testPersistence() {
        val element = Element("configuration")
        config.let {
            it.requirements = "requirements.yml"
            it.deps = false
            it.rolesDir = "roles"
            it.force = true
            it.command = "/path/to/ansible-galaxy"
            it.rawOpts = "one \"two\""
            it.workDir = "/path/to/project"
            it.writeExternal(element)
        }
        GalaxyRunConfiguration(project, factory, "Persistence Test").let {
            it.readExternal(element)
            assertEquals("requirements.yml", it.requirements)
            assertEquals(false, it.deps)
            assertEquals("roles", it.rolesDir)
            assertEquals(true, it.force)
            assertEquals("/path/to/ansible-galaxy", it.command)
            assertEquals("one \"two\"", it.rawOpts)
            assertEquals("/path/to/project", it.workDir)
        }
    }

    /**
     * Test the `command` property default value.
     */
    fun testCommandDefault() {
        config.let {
            it.command = ""
            assertEquals("ansible-playbook", it.command)
        }
    }
}


/**
 * Unit tests for the GalaxySettingsEditor class.
 */
internal class GalaxySettingsEditorTest : BasePlatformTestCase() {

    private lateinit var editor: GalaxySettingsEditor

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        editor = GalaxySettingsEditor()
    }

    // TODO: https://github.com/JetBrains/intellij-ui-test-robot

    /**
     * Test the primary constructor.
     */
    fun testConstructor() {
        // Just a smoke test.
        assertNotNull(editor.component)
    }
}
