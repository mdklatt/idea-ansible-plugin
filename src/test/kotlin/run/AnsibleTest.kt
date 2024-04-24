/**
 * Unit tests for the Ansible.kt.
 */
package dev.mdklatt.idea.ansible.run

import com.intellij.openapi.util.io.toNioPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.mdklatt.idea.ansible.AnsibleSettingsComponent
import dev.mdklatt.idea.ansible.AnsibleSettingsState
import dev.mdklatt.idea.ansible.InstallType
import dev.mdklatt.idea.ansible.getAnsibleSettings
import org.testcontainers.containers.GenericContainer
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.utility.DockerImageName
import java.nio.file.Paths


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
        ansibleSettings.loadState(AnsibleSettingsState())  // reset to defaults
        super.tearDown()
    }

    internal companion object {
        val ansibleVenv = "/opt/ansible"
        val ansibleImage by lazy {
            // There does not seem to be a way to invoke the image builder
            // directly, so instantiating a container with it seems to be the
            // only way to get a built image. It is not necessary to actually
            // start the container.
            val builder = ImageFromDockerfile().also {
                // It should be possible to build the image from a Dockerfile,
                // but this method does not work. Testcontainers documentation
                // isn't very helpful.
                // TODO: it.withDockerfile(getTestPath("Dockerfile").toNioPath())
                it.withDockerfileFromBuilder { builder ->
                    builder.from("linuxserver/openssh-server:version-9.3_p2-r0")
                    builder.run("apk add gcc libffi-dev musl-dev python3 python3-dev")
                    builder.run("python3 -m venv $ansibleVenv")
                    builder.run(". ${ansibleVenv}/bin/activate && python3 -m pip install ansible==6.1.0")
                    builder.build()
                }
            }
            GenericContainer(builder).dockerImageName
        }

        /**
         * Get the path to a test resource file.
         *
         * @param file: file path relative to the resources directory
         * @return absolute file path
         */
        fun getTestPath(file: String) = this::class.java.getResource(file)?.path ?: ""
    }
}
