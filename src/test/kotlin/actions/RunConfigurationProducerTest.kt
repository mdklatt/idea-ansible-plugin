package dev.mdklatt.idea.ansible.actions

import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.utils.vfs.getPsiFile
import dev.mdklatt.idea.ansible.run.AnsibleRunConfiguration
import dev.mdklatt.idea.ansible.run.GalaxyRunConfiguration
import dev.mdklatt.idea.ansible.run.PlaybookRunConfiguration


/**
 * Base class for configuration producer unit tests.
 */
internal abstract class AnsibleConfigurationProducerTest: BasePlatformTestCase() {

    protected abstract val yamlFile: String
    protected lateinit var context: ConfigurationContext

    /**
     * Set the path for test resource files.
     *
     * @return resource path
     */
    override fun getTestDataPath() = "src/test/resources"

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        val file = myFixture.copyFileToProject(yamlFile)
        val location = PsiLocation(file.getPsiFile(project))
        context = ConfigurationContext.createEmptyContextForLocation(location)
    }
}


/**
 * Unit tests for the PlaybookConfigurationAction class.
 */
internal class PlaybookConfigurationProducerTest: AnsibleConfigurationProducerTest() {

    override val yamlFile = "playbook.yml"
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
        val configuration = producer.createConfigurationFromContext(context)
        (configuration?.configuration as PlaybookRunConfiguration).let {
            assertTrue(producer.isConfigurationFromContext(it, context))
            assertSameElements(setOf(testFile), it.playbooks)
        }
    }
}


/**
 * Unit tests for the GalaxyConfigurationAction class.
 */
internal class GalaxyConfigurationProducerTest: AnsibleConfigurationProducerTest() {

    override val yamlFile = "requirements.yml"
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
        val configuration = producer.createConfigurationFromContext(context)
        (configuration?.configuration as GalaxyRunConfiguration).let {
            assertTrue(producer.isConfigurationFromContext(it, context))
            assertEquals(testFile, it.requirements)
        }
    }
}
