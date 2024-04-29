/**
 * Unit tests for the Playbook.kt.
 */
package dev.mdklatt.idea.ansible.run

import com.intellij.execution.*
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.mdklatt.idea.ansible.InstallType
import dev.mdklatt.idea.ansible.getAnsibleSettings
import kotlin.test.assertContentEquals
import kotlin.io.path.Path
import org.jdom.Element


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
            assertEquals("ansible-playbook", it.ansibleCommand)
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
            assertEquals("ansible-playbook", it.ansibleCommand)
            assertTrue(it.uid.isNotBlank())
            assertEquals(emptyList<String>(), it.playbooks)
            assertEquals(emptyList<String>(), it.inventory)
            assertEquals("", it.host)
            assertFalse(it.sudoPassPrompt)
            assertEquals(emptyList<String>(), it.tags)
            assertEquals(emptyList<String>(), it.variables)
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
            it.rawOpts = "one \"two\""
            it.workDir = "/path/to/project"
            config.writeExternal(element)
        }
        PlaybookRunConfiguration(project, factory, "Persistence Text").let {
            it.readExternal(element)
            assertEquals(config.playbooks, it.playbooks)
            assertEquals(config.inventory, it.inventory)
            assertEquals(config.host, it.host)
            assertContentEquals(config.sudoPass.value, it.sudoPass.value)
            assertEquals(config.tags, it.tags)
            assertEquals(config.variables, it.variables)
            assertEquals(config.rawOpts, it.rawOpts)
            assertEquals(config.workDir, it.workDir)
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
internal class PlaybookCommandLineStateTest : AnsibleCommandLineStateTest() {

    private val command = "ansible-playbook"
    private val inventory = "hosts.yml"
    private var playbook = "playbook.yml"
    private lateinit var configuration: PlaybookRunConfiguration
    private lateinit var state: PlaybookCommandLineState

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        val factory = PlaybookConfigurationFactory(AnsibleConfigurationType())
        val settings = RunManager.getInstance(project).createConfiguration("Playbook Test", factory)
        configuration = (settings.configuration as PlaybookRunConfiguration).also {
            it.inventory = mutableListOf(inventory)
            it.playbooks = mutableListOf(playbook)
            it.workDir = Path(getTestPath("/${playbook}")).parent.toString()
        }
        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val environment = ExecutionEnvironmentBuilder.create(executor, settings).build()
        state = PlaybookCommandLineState(environment)
    }

    /**
     * Test the getCommand() method.
     */
    fun testGetCommand() {
        val command = "$command --inventory $inventory $playbook"
        assertEquals(command, state.getCommand().commandLineString)
    }

    /**
     * Test local execution.
     */
    fun testExecLocal() {
        // This uses the default test installation, which is a local Python
        // virtualenv (see AnsibleCommandLineStateTest).
        val env = state.environment
        state.execute(env.executor, env.runner).processHandler.let {
            it.startNotify()
            it.waitFor()
            assertEquals(0, it.exitCode)
        }
    }

    /**
     * Test Docker execution.
     */
    fun testExecDocker() {
        ansibleSettings.state.let {
            it.dockerImage = ansibleImage
            it.installType = InstallType.SYSTEM
            it.ansibleLocation = "/opt/ansible/bin/ansible"
        }
        val env = state.environment
        state.execute(env.executor, env.runner).processHandler.let {
            it.startNotify()
            it.waitFor()
            assertEquals(0, it.exitCode)
        }
    }

    /**
     * Test Docker execution in a virtualenv.
     */
    fun testExecDockerVenv() {
        getAnsibleSettings(project).state.let {
            it.dockerImage = ansibleImage
            it.installType = InstallType.VIRTUALENV
            it.ansibleLocation = "/opt/ansible"
        }
        val env = state.environment
        state.execute(env.executor, env.runner).processHandler.let {
            it.startNotify()
            it.waitFor()
            assertEquals(0, it.exitCode)
        }
    }
}
