/**
 * Unit tests for the Galaxy module.
 */
package software.mdklatt.idea.ansible.test.run

import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.getOrCreate
import org.jdom.Element
import software.mdklatt.idea.ansible.run.*


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.


/**
 * Unit tests for the GalaxyRunSettings class.
 */
class GalaxySettingsTest : BasePlatformTestCase() {

    private lateinit var element: Element
    private lateinit var settings: GalaxySettings

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        element = Element("configuration")
        settings = GalaxySettings().apply {
            requirements = "requirements.yml"
            deps = false
            force = true
            rolesDir = "roles"
            command = "/path/to/ansible-galaxy"
            rawOpts = "one \"two\""
            workDir = "/path/to/project"
        }
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
        element.getOrCreate("ansible-galaxy").let {
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
class GalaxyConfigurationFactoryTest : BasePlatformTestCase() {

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
class GalaxyRunConfigurationTest : BasePlatformTestCase() {

    private lateinit var element: Element
    private lateinit var config: GalaxyRunConfiguration

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        element = Element("configuration")
        element.getOrCreate("ansible-galaxy").let {
            JDOMExternalizerUtil.writeField(it, "rolesDir", "roles")
            JDOMExternalizerUtil.writeField(it, "force", "true")
        }
        val factory = GalaxyConfigurationFactory(AnsibleConfigurationType())
        config = GalaxyRunConfiguration(project, factory, "Ansible Galaxy Test")
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
        element.getOrCreate("ansible-galaxy").let {
            assertTrue(JDOMExternalizerUtil.readField(it, "id", "").isNotEmpty())
        }
    }
}


/**
 * Unit tests for the GalaxySettingsEditor class.
 */
class GalaxySettingsEditorTest : BasePlatformTestCase() {
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
