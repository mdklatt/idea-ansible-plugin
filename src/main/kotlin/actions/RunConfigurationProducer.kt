package dev.mdklatt.idea.ansible.actions

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import dev.mdklatt.idea.ansible.run.AnsibleConfigurationType
import dev.mdklatt.idea.ansible.run.PlaybookConfigurationFactory
import dev.mdklatt.idea.ansible.run.PlaybookRunConfiguration
import kotlin.io.path.Path
import org.jetbrains.yaml.YAMLFileType
import kotlin.io.path.name


/**
 * Create a Playbook run configuration from a context menu.
 *
 * When this is registered as a runConfigurationProducer extension point,
 * opening a context men (i.e. right-clicking) on a YAML file will give the
 * option to crate a run configuration for it.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#creating-a-run-configuration-from-context">Creating a Run Configuration from Context</a>
 */
class PlaybookConfigurationProducer: LazyRunConfigurationProducer<PlaybookRunConfiguration>() {
    /**
     * Get a configuration factory for this producer.
     *
     * @return configuration factory
     */
    override fun getConfigurationFactory() = PlaybookConfigurationFactory(AnsibleConfigurationType())

    /**
     * Initialize a new configuration.
     *
     * @return true if set up was successful
     */
    override fun setupConfigurationFromContext(
        configuration: PlaybookRunConfiguration,
        context: ConfigurationContext,
        element: Ref<PsiElement>
    ): Boolean {
        val yamlFile = context.psiLocation?.containingFile ?: return false
        if (yamlFile.fileType !is YAMLFileType) {
            // TODO: Need more extensive validation.
            return false
        }
        val yamlPath = Path(yamlFile.virtualFile?.path ?: return false)
        configuration.let {
            it.name = "Run playbook ${yamlPath.name}"
            it.playbooks = mutableListOf(yamlPath.name)
            it.workDir = yamlPath.parent.toString()
        }
         return true
    }

    /**
     * Determine if a configuration was created by a specific context.
     *
     * @param configuration target run configuration
     * @param context configuration context
     * @return true if this configuration is from the context
     */
    override fun isConfigurationFromContext(configuration: PlaybookRunConfiguration, context: ConfigurationContext): Boolean {
        // According to the SDK docs, the intent of this method is to allow
        // reuse of an existing run configuration within a context. Not sure
        // under which situations this is called, or what the return value
        // should be. Here, reuse does not make sense because the same playbook
        // file (i.e. context) can have multiple run configuration with
        // different parameters.
        return false
    }
}
