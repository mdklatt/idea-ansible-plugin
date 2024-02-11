package dev.mdklatt.idea.ansible.actions

import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.utils.vfs.getPsiFile
import dev.mdklatt.idea.ansible.run.GalaxyRunConfiguration
import dev.mdklatt.idea.ansible.run.PlaybookRunConfiguration


/**
 * Base class for configuration producer unit tests.
 */
internal abstract class AnsibleConfigurationProducerTest: BasePlatformTestCase() {
    /**
     * Set the path for test resource files.
     *
     * @return resource path
     */
    override fun getTestDataPath() = "src/test/resources"

    /**
     *
     * @param yamlFile:
     * @returns context for test file
     */
    fun context(yamlFile: String): ConfigurationContext {
        val file = myFixture.copyFileToProject(yamlFile)
        val location = PsiLocation(file.getPsiFile(project))
        return ConfigurationContext.createEmptyContextForLocation(location)
    }
}


/**
 * Unit tests for the PlaybookConfigurationAction class.
 */
internal class PlaybookConfigurationProducerTest: AnsibleConfigurationProducerTest() {

    private lateinit var producer: PlaybookConfigurationProducer

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        producer = PlaybookConfigurationProducer()
    }

    /**
     * Test the createConfigurationFromContext() method.
     */
    fun testCreateConfigurationFromContext() {
        val testFile = "playbook.yml"
        val configuration = producer.createConfigurationFromContext(context(testFile))
        (configuration?.configuration as PlaybookRunConfiguration).let {
            assertSameElements(setOf(testFile), it.playbooks)
        }
    }
}


/**
 * Unit tests for the GalaxyConfigurationAction class.
 */
internal class GalaxyConfigurationProducerTest: AnsibleConfigurationProducerTest() {

    private lateinit var producer: GalaxyConfigurationProducer

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        producer = GalaxyConfigurationProducer()
    }

    /**
     * Test the createConfigurationFromContext() method.
     */
    fun testCreateConfigurationFromContext() {
        val testFile = "requirements.yml"
        val configuration = producer.createConfigurationFromContext(context(testFile))
        (configuration?.configuration as GalaxyRunConfiguration).let {
            assertEquals(testFile, it.requirements)
        }
    }
}
