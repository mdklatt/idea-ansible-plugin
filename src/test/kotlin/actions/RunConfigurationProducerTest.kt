package dev.mdklatt.idea.ansible.actions

import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.utils.vfs.getPsiFile
import dev.mdklatt.idea.ansible.run.PlaybookRunConfiguration


/**
 * Unit tests for the PlaybookConfigurationAction class.
 */
internal class PlaybookConfigurationProducerTest: BasePlatformTestCase() {

    private lateinit var producer: PlaybookConfigurationProducer

    /**
     * Per-test initialization.
     */
    override fun setUp() {
        super.setUp()
        producer = PlaybookConfigurationProducer()
    }

    /**
     * Set the path for test resource files.
     *
     * @return resource path
     */
    override fun getTestDataPath() = "src/test/resources"

    /**
     * Test the createConfigurationFromContext() method.
     */
    fun testCreateConfigurationFromContext() {
        val file = myFixture.copyFileToProject("playbook.yml")
        val location = PsiLocation(file.getPsiFile(project))
        val context = ConfigurationContext.createEmptyContextForLocation(location)
        val configuration = producer.createConfigurationFromContext(context)
        (configuration?.configuration as PlaybookRunConfiguration).let {
            assertSameElements(setOf("playbook.yml"), it.playbooks)
        }
    }
}
