/**
 * Unit tests for Galaxy.kt.
 */
package dev.mdklatt.idea.ansible.run

import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.io.delete
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
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
            assertEquals("", it.collectionsDir)
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
            it.collectionsDir = "collections"
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
            assertEquals("collections", it.collectionsDir)
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
            assertEquals("ansible-galaxy", it.command)
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


/**
 * Unit tests for the GalaxyCommandLineState class.
 */
internal class GalaxyCommandLineStateTest : BasePlatformTestCase() {

    private var tmpDir: Path? = null
    private lateinit var configuration: GalaxyRunConfiguration
    private lateinit var state: GalaxyCommandLineState

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        val factory = GalaxyConfigurationFactory(AnsibleConfigurationType())
        val runConfig = RunManager.getInstance(project).createConfiguration("Galaxy Test", factory)
        configuration = (runConfig.configuration as GalaxyRunConfiguration)
        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val environment = ExecutionEnvironmentBuilder.create(executor, runConfig).build()
        state = GalaxyCommandLineState(environment)
    }

    /**
     * Per-test cleanup.
     */
    override fun tearDown() {
        tmpDir?.delete()
        super.tearDown()
    }

    /**
     * Test the getCommand() method for default install directories.
     */
    fun testGetCommandDefaultDirs() {
        configuration.let {
            it.requirements = "requirements.txt"
            it.force = true
        }
        val command = "ansible-galaxy install --force-with-deps -r requirements.txt"
        assertEquals(command, state.getCommand().commandLineString)
    }

    /**
     * Test the getCommand() method for non-default install directories.
     */
    fun testGetCommandCustomDirs() {
        configuration.let {
            it.collectionsDir = "abc"
            it.rolesDir = "xyz"
            it.requirements = "requirements.yml"
        }
        val command = "sh -c \"" +
            "ansible-galaxy collection install -r requirements.yml -p abc && " +
            "ansible-galaxy role install -r requirements.yml -p xyz\""
        assertEquals(command, state.getCommand().commandLineString)
    }

    /**
     * Test the createProcess() method.
     */
    fun testCreateProcess() {
        // Indirectly test createProcess() by executing the configuration.
        tmpDir = createTempDirectory()
        configuration.let {
            it.command = ".venv/bin/ansible-galaxy"
            it.requirements = getTestPath("/ansible/requirements.yml")
            it.rolesDir = tmpDir.toString()
            it.collectionsDir = it.rolesDir
        }
        val env = state.environment
        state.execute(env.executor, env.runner).processHandler.let {
            it.startNotify()
            it.waitFor()
            assertEquals(0, it.exitCode)
            assertTrue(tmpDir!!.resolve("mdklatt.tmpdir").toFile().isDirectory)
            assertTrue(tmpDir!!.resolve("ansible_collections/ansible/posix").toFile().isDirectory)
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
