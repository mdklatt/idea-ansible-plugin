/**
 * Unit tests for the Ansible.kt.
 */
package dev.mdklatt.idea.ansible.run

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.mdklatt.idea.ansible.AnsibleSettingsComponent
import dev.mdklatt.idea.ansible.AnsibleSettingsState
import dev.mdklatt.idea.ansible.InstallType
import dev.mdklatt.idea.ansible.getAnsibleSettings
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull


/**
 * Unit tests for the AnsibleConfigurationType class.
 */
internal class AnsibleConfigurationTypeTest {

    private var type = AnsibleConfigurationType()

    /**
     * Test the id property.
     */
    @Test
    fun testId() {
        assertTrue(type.id.isNotBlank())
    }

    /**
     * Test the icon property.
     */
    @Test
    fun testIcon() {
        assertNotNull(type.icon)
    }

    /**
     * Test the configurationTypeDescription property.
     */
    @Test
    fun testConfigurationTypeDescription() {
        assertTrue(type.configurationTypeDescription.isNotEmpty())
    }

    /**
     * Test the displayName property.
     */
    @Test
    fun testDisplayName() {
        assertTrue(type.displayName.isNotEmpty())
    }

    /**
     * Test the configurationFactories property.
     */
    @Test
    fun testConfigurationFactories() {
        assertTrue(type.configurationFactories.isNotEmpty())
    }
}


/**
 * Base class for CommandLineState test fixtures.
 *
 * IDEA platform tests use JUnit3, so method names are used to determine
 * behavior instead of annotations. Notably, test classes are *not* constructed
 * before each test, so setUp() methods should be used for initialization.
 * Also, test functions must be named `testXXX` or they will not be found
 * during automatic discovery.
 */
internal abstract class AnsibleCommandLineStateTest : BasePlatformTestCase() {

    private lateinit var ansibleSettings: AnsibleSettingsComponent

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        ansibleSettings = getAnsibleSettings(project).also {
            // Use a temporary virtualenv installation (see build.gradle.kts)
            // of Ansible for test execution.
            it.state.installType = InstallType.VIRTUALENV
            it.state.ansibleLocation = ".venv"
        }
    }

    /**
     * Per-test cleanup.
     */
    override fun tearDown() {
        ansibleSettings.loadState(AnsibleSettingsState())
        super.tearDown()
    }

    internal companion object {
        /**
         * Get the path to a test resource file.
         *
         * @param file: file path relative to the resources directory
         * @return absolute file path
         */
        fun getTestPath(file: String) = this::class.java.getResource(file)?.path ?: ""
    }
}
