/**
 * Unit tests for Galaxy.kt.
 */
package dev.mdklatt.idea.ansible

import com.intellij.testFramework.fixtures.BasePlatformTestCase


// The IDEA platform tests use JUnit3, so method names are used to determine
// behavior instead of annotations. Notably, test classes are *not* constructed
// before each test, so setUp() methods should be used for initialization.
// Also, test functions must be named `testXXX` or they will not be found
// during automatic discovery.


/**
 * Unit tests for the AnsibleSettingsState class.
 */
internal class AnsibleSettingsStateTest : BasePlatformTestCase() {

    private lateinit var state: AnsibleSettingsState

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        state = AnsibleSettingsState()
    }

    /**
     * Test the `ansibleLocation` property.
     */
    fun testAnsibleLocation() {
        assertEquals("ansible", state.ansibleLocation)
        state.ansibleLocation = "abc"
        assertEquals("abc", state.ansibleLocation)
    }

    /**
     * Test the `ansibleLocation` property.
     */
    fun testConfigFile() {
        assertEquals(state.configFile, null)
    }

    /**
     * Test the `ansibleLocation` property.
     */
    fun testInstallType() {
        assertEquals(state.installType, InstallType.SYSTEM)
        state.installType = InstallType.VIRTUALENV
        assertEquals(InstallType.VIRTUALENV, state.installType)
    }
}


internal class AnsibleSettingsComponentTest: BasePlatformTestCase() {

    private lateinit var settings: AnsibleSettingsComponent

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        settings = AnsibleSettingsComponent()
    }

    /**
     * Test the `ansibleResolvePath()` method with a system path.
     */
    fun testResolveAnsiblePath() {
        settings.state.installType = InstallType.SYSTEM
        assertEquals("abc", settings.resolveAnsiblePath("abc"))
    }

    /**
     * Test the `ansibleResolvePath()` method with an explicit system path.
     */
    fun testResolveAnsiblePathExplicit() {
        settings.state.installType = InstallType.SYSTEM
        settings.state.ansibleLocation = "/usr/bin/ansible"
        assertEquals("/usr/bin/abc", settings.resolveAnsiblePath("abc"))
    }

    /**
     * Test the `ansibleResolvePath()` method for a Python virtualenv.
     */
    fun testResolveAnsiblePathVenv() {
        settings.state.installType = InstallType.VIRTUALENV
        settings.state.ansibleLocation = "venv"
        assertEquals("abc", settings.resolveAnsiblePath("abc"))
    }

    /**
     * Test the `ansibleResolvePath()` method for a Docker container.
     */
    fun testResolveAnsiblePathDocker() {
        settings.state.installType = InstallType.DOCKER
        settings.state.ansibleLocation = "/usr/bin/ansible"
        assertEquals("/usr/bin/abc", settings.resolveAnsiblePath("abc"))
    }
}


internal class AnsibleSettingsConfigurableTest : BasePlatformTestCase() {

    private lateinit var configurable: AnsibleSettingsConfigurable

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        configurable = AnsibleSettingsConfigurable(project)
    }

    /**
     * Test the displayName attribute.
     */
    fun testDisplayName() {
        assertEquals("Ansible", configurable.displayName)
    }
}