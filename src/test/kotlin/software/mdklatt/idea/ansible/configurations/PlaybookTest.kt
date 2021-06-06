/**
 * Unit tests for the Playbook module.
 */
package software.mdklatt.idea.ansible.configurations

import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.getOrCreate
import org.jdom.Element
import org.junit.Assert.assertArrayEquals


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.


/**
 * Unit tests for the PlaybookRunSettings class.
 */
internal class PlaybookSettingsTest : BasePlatformTestCase() {

    private lateinit var settings: PlaybookSettings
    private lateinit var element: Element

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        settings = PlaybookSettings().apply {
            playbooks = listOf("playbook.yml")
            inventory = listOf("hosts.yml")
            host = "hostname"
            sudoPass = charArrayOf('P', 'A', 'S', 'S')
            sudoPrompt = false
            variables = listOf("key1=val1", "key2=val2")
            tags = listOf("abc", "xyz")
            command = "/path/to/ansible-playbook"
            rawOpts = "one \"two\""
            workDir = "/path/to/project"
        }
        element = Element("configuration")
    }

    /**
     * Per-test cleanup.
     */
    override fun tearDown() {
        settings.apply {
            // Save empty password to remove it from the keychain.
            sudoPass = charArrayOf()
            save(element)
        }
        super.tearDown()
    }

    /**
     * Test the constructor.
     */
    fun testConstructor() {
        PlaybookSettings().apply {
            assertEquals(emptyList<String>(), playbooks)
            assertEquals(emptyList<String>(), inventory)
            assertEquals("", host)
            assertFalse(sudoPrompt)
            assertEquals(emptyList<String>(), tags)
            assertEquals(emptyList<String>(), variables)
            assertEquals("ansible-playbook", command)
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
        PlaybookSettings().apply {
            load(element)
            assertEquals(listOf("playbook.yml"), playbooks)
            assertEquals(listOf("hosts.yml"), inventory)
            assertEquals("hostname", host)
            assertArrayEquals(charArrayOf('P', 'A', 'S', 'S'), sudoPass)
            assertEquals(false, sudoPrompt)
            assertEquals(listOf("abc", "xyz"), tags)
            assertEquals(listOf("key1=val1", "key2=val2"), variables)
            assertEquals("/path/to/ansible-playbook", command)
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
            assertEquals("ansible-playbook", command)
        }
    }
}


/**
 * Unit tests for the PlaybookConfigurationFactory class.
 */
internal class PlaybookConfigurationFactoryTest : BasePlatformTestCase() {

    private lateinit var factory: PlaybookConfigurationFactory

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        factory = PlaybookConfigurationFactory(AnsibleConfigurationType())
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
 * Unit tests for the PlaybookRunConfiguration class.
 */
internal class PlaybookRunConfigurationTest : BasePlatformTestCase() {

    private lateinit var config: PlaybookRunConfiguration
    private lateinit var element: Element

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        val factory = PlaybookConfigurationFactory(AnsibleConfigurationType())
        config = PlaybookRunConfiguration(project, factory, "Ansible Playbook Test")
        element = Element("configuration")
        element.getOrCreate(config.settings.xmlTagName).let {
            JDOMExternalizerUtil.writeField(it, "host", "hostname")
            JDOMExternalizerUtil.writeField(it, "sudoPrompt", "true")
        }
    }

    override fun tearDown() {
        config.settings.apply {
            // Save empty password to remove it from the keychain.
            sudoPass = charArrayOf()
            save(element)
        }
        super.tearDown()
    }

    fun testConstructor() {
        assertEquals("Ansible Playbook Test", config.name)
    }

    /**
     * Test the readExternal() method.
     */
    fun testReadExternal() {
        config.apply {
            readExternal(element)
            assertEquals("hostname", settings.host)
            assertEquals(true, settings.sudoPrompt)
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
 * Unit tests for the PlaybookSettingsEditor class.
 */
internal class PlaybookSettingsEditorTest : BasePlatformTestCase() {
    /**
     * Test the constructor.
     */
    fun testConstructor() {
        PlaybookSettingsEditor(project).apply {
            assertTrue(playbooks.text.isEmpty())
            assertTrue(inventory.text.isEmpty())
            assertTrue(host.text.isEmpty())
            assertTrue(password.password.joinToString("").isEmpty())
            assertFalse(passwordPrompt.isSelected)
            assertTrue(tags.text.isEmpty())
            assertTrue(variables.text.isEmpty())
            assertTrue(command.text.isEmpty())
            assertTrue(rawOpts.text.isEmpty())
            assertTrue(workDir.text.isEmpty())
        }
    }
}
