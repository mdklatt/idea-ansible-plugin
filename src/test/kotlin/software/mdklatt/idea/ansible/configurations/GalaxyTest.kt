/**
 * Unit tests for the Galaxy module.
 */
package software.mdklatt.idea.ansible.configurations

import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.getOrCreate
import org.jdom.Element


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.


/**
 * Unit tests for the GalaxyRunSettings class.
 */
internal class GalaxySettingsTest : BasePlatformTestCase() {

    private lateinit var settings: GalaxySettings
    private lateinit var element: Element

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        settings = GalaxySettings().apply {
            requirements = "requirements.yml"
            deps = false
            force = true
            rolesDir = "roles"
            command = "/path/to/ansible-galaxy"
            rawOpts = "one \"two\""
            workDir = "/path/to/project"
        }
        element = Element("configuration")
    }

    /**
     * Test the constructor.
     */
    fun testConstructor() {
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
     * Test round-trip write/read of settings.
     */
    fun testPersistence() {
        settings.save(element)
        element.getOrCreate(settings.xmlTagName).let {
            assertTrue(JDOMExternalizerUtil.readField(it, "id", "").isNotEmpty())
        }
        GalaxySettings().apply {
            load(element)
            assertEquals("requirements.yml", requirements)
            assertEquals("roles", rolesDir)
            assertEquals(false, deps)
            assertEquals(true, force)
            assertEquals("/path/to/ansible-galaxy", command)
            assertEquals("one \"two\"", rawOpts)
            assertEquals("/path/to/project", workDir)
        }
    }

    /**
     * Test the `command` attribute default value.
     */
    fun testCommandDefault() {
        settings.apply {
            command = ""
            assertEquals("ansible-galaxy", command)
        }
    }
}


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
}


/**
 * Unit tests for the GalaxyRunConfiguration class.
 */
internal class GalaxyRunConfigurationTest : BasePlatformTestCase() {

    private lateinit var config: GalaxyRunConfiguration
    private lateinit var element: Element

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        val factory = GalaxyConfigurationFactory(AnsibleConfigurationType())
        config = GalaxyRunConfiguration(project, factory, "Ansible Galaxy Test")
        element = Element("configuration")
        element.getOrCreate(config.settings.xmlTagName).let {
            JDOMExternalizerUtil.writeField(it, "rolesDir", "roles")
            JDOMExternalizerUtil.writeField(it, "force", "true")
        }
    }

    fun testConstructor() {
        assertEquals("Ansible Galaxy Test", config.name)
    }

    /**
     * Test the readExternal() method.
     */
    fun testReadExternal() {
        config.apply {
            readExternal(element)
            assertEquals("roles", settings.rolesDir)
            assertEquals(true, settings.force)
        }
    }

    /**
     * Test the writeExternal() method.
     */
    fun testWriteExternal() {
        config.writeExternal(element)
        element.getOrCreate(config.settings.xmlTagName).let {
            assertTrue(JDOMExternalizerUtil.readField(it, "id", "").isNotEmpty())
        }
    }
}


/**
 * Unit tests for the GalaxySettingsEditor class.
 */
internal class GalaxySettingsEditorTest : BasePlatformTestCase() {
    /**
     * Test the constructor.
     */
    fun testConstructor() {
        GalaxySettingsEditor(project).apply {
            assertTrue(requirements.text.isEmpty())
            assertTrue(rolesDir.text.isEmpty())
            assertFalse(deps.isSelected)
            assertFalse(force.isSelected)
            assertTrue(command.text.isEmpty())
            assertTrue(rawOpts.text.isEmpty())
            assertTrue(workDir.text.isEmpty())
        }
    }
}
