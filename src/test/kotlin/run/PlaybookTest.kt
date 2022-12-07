/**
 * Unit tests for the Playbook.kt.
 */
package dev.mdklatt.idea.ansible.run

import com.intellij.execution.*
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jdom.Element
import kotlin.test.assertContentEquals


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.


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
 * Unit tests for the PlaybookRunConfiguration class.
 */
internal class PlaybookRunConfigurationTest : BasePlatformTestCase() {

    private lateinit var factory: PlaybookConfigurationFactory
    private lateinit var config: PlaybookRunConfiguration

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        factory = PlaybookConfigurationFactory(AnsibleConfigurationType())
        config = PlaybookRunConfiguration(project, factory, "Ansible Playbook Test")
    }

    /**
     * Per-test cleanup.
     */
    override fun tearDown() {
        config.sudoPass.value = null  // remove from credential store
        super.tearDown()
    }

    fun testConstructor() {
        config.let {
            assertTrue(it.uid.isNotBlank())
            assertEquals(emptyList<String>(), it.playbooks)
            assertEquals(emptyList<String>(), it.inventory)
            assertEquals("", it.host)
            assertFalse(it.sudoPassPrompt)
            assertEquals(emptyList<String>(), it.tags)
            assertEquals(emptyList<String>(), it.variables)
            assertEquals("ansible-playbook", it.command)
            assertEquals("", it.virtualEnv)
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
            it.playbooks = mutableListOf("playbook.yml")
            it.inventory = mutableListOf("hosts.yml")
            it.host = "hostname"
            it.sudoPass.value = charArrayOf('1', '2', '3')
            it.tags = listOf("abc", "xyz")
            it.variables = listOf("key1=val1", "key2=val2")
            it.command = "/path/to/ansible-playbook"
            it.virtualEnv = "/path/to/venv"
            it.rawOpts = "one \"two\""
            it.workDir = "/path/to/project"
            config.writeExternal(element)
        }
        PlaybookRunConfiguration(project, factory, "Persistence Text").let {
            it.readExternal(element)
            assertEquals(listOf("playbook.yml"), it.playbooks)
            assertEquals(listOf("hosts.yml"), it.inventory)
            assertEquals("hostname", it.host)
            assertContentEquals(config.sudoPass.value, it.sudoPass.value)
            assertEquals(listOf("abc", "xyz"), it.tags)
            assertEquals(listOf("key1=val1", "key2=val2"), it.variables)
            assertEquals("/path/to/ansible-playbook", it.command)
            assertEquals("/path/to/venv", it.virtualEnv)
            assertEquals("one \"two\"", it.rawOpts)
            assertEquals("/path/to/project", it.workDir)
        }
    }

    /**
     * Test behavior of the sudoPassPrompt field.
     */
    fun testSudoPassPrompt() {
        val element = Element("configuration")
        config.let {
            it.sudoPass.value = charArrayOf('1', '2', '3')
            it.sudoPassPrompt = true
            it.writeExternal(element)
        }
        PlaybookRunConfiguration(project, factory, "Password Prompt Test").let {
            // Enabling the password prompt should remove the stored password.
            it.readExternal(element)
            assertNull(config.sudoPass.value)
            assertNull(it.sudoPass.value)
            assertTrue(it.sudoPassPrompt)
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
 * Unit tests for the PlaybookEditor class.
 */
internal class PlaybookEditorTest : BasePlatformTestCase() {

    private lateinit var editor: PlaybookEditor

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        editor = PlaybookEditor()
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


/**
 * Unit tests for the PlaybookCommandLineState class.
 */
internal class PlaybookCommandLineStateTest : BasePlatformTestCase() {

    private val ansible = "ansible-playbook"
    private val inventory = getTestPath("/ansible/hosts.yml")
    private var playbook = getTestPath("/ansible/playbook.yml")
    private lateinit var state: PlaybookCommandLineState

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        val factory = PlaybookConfigurationFactory(AnsibleConfigurationType())
        val runConfig = RunManager.getInstance(project).createConfiguration("Playbook Test", factory)
        (runConfig.configuration as PlaybookRunConfiguration).also {
            it.command = ansible
            it.virtualEnv = ".venv"
            it.inventory = mutableListOf(inventory)
            it.playbooks = mutableListOf(playbook)
        }
        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val environment = ExecutionEnvironmentBuilder.create(executor, runConfig).build()
        state = PlaybookCommandLineState(environment)
    }

    /**
     * Test the getCommand() method.
     */
    fun testGetCommand() {
        val command = "$ansible --inventory $inventory $playbook"
        assertEquals(command, state.getCommand().commandLineString)
    }

    /**
     * Test the createProcess() method.
     */
    fun testCreateProcess() {
        // Indirectly test createProcess() by executing the configuration.
        val env = state.environment
        state.execute(env.executor, env.runner).processHandler.let {
            it.startNotify()
            it.waitFor()
            assertEquals(0, it.exitCode)
        }
    }

    companion object {
        /**
         * Get the path to a test resource file.
         *
         * @param file: file path relative to the resources directory
         * @return absolute file path
         */
        private fun getTestPath(file: String) = this::class.java.getResource(file)?.path ?: ""
    }
}
