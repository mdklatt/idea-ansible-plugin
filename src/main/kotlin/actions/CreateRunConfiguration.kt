package dev.mdklatt.idea.ansible.actions

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.configurations.RuntimeConfigurationWarning
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.PsiFile
import dev.mdklatt.idea.ansible.run.*
import org.jetbrains.yaml.YAMLFileType
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.relativeTo


/**
 * Create an Ansible run configuration for a project file.
 */
abstract class CreateRunConfiguration<
        Options : AnsibleOptions,
        Config : AnsibleRunConfiguration<Options>,
        Editor : AnsibleEditor<Options, Config>> : AnAction() {

    protected val logger = Logger.getInstance(this::class.java)  // runtime class resolution
    protected lateinit var event: AnActionEvent
    protected lateinit var projectPath: Path
    protected lateinit var targetFile: PsiFile
    protected lateinit var targetPath: Path

    /**
     * Execute the action.
     *
     * @param event carries information on the invocation place
     */
    override fun actionPerformed(event: AnActionEvent) {
        this.event = event
        event.project?.let {
            projectPath = Path(it.basePath.orEmpty())
            targetFile = event.getData(CommonDataKeys.PSI_FILE)?.containingFile ?: return
            targetPath = Path(targetFile.virtualFile?.canonicalPath ?: return).relativeTo(projectPath)
            if (!isValidTarget()) {
                // Validation is not yet reliable enough to make this fatal, so
                // display a warning dialog and let the user continue.
                val message = "Target does not appear to be a valid Ansible file: $targetPath"
                logger.warn(message)
                Messages.showMessageDialog(it, message, event.presentation.text, Messages.getWarningIcon())
            }
            createConfiguration()
        }
    }

    /**
     * Create a new run configuration from a user dialog.
     */
    private fun createConfiguration() {
        val manager = RunManager.getInstance(event.project!!)
        val settings = manager.createConfiguration(getName(), getFactory())
        setDefaults(settings)
        val configuration = getConfiguration()
        val editor = getEditor()
        editor.resetFrom(getConfiguration())
        if (createDialog(editor, configuration, settings).showAndGet()) {
            editor.applyTo(configuration)
            manager.setUniqueNameIfNeeded(settings)
            logger.info("Created run configuration ${settings.name}")
            manager.addConfiguration(settings)
            manager.selectedConfiguration = settings
        }
    }

    /**
     * User dialog for creating a new run configuration.
     *
     * @return UI component
     */
    private fun createDialog(
        editor: SettingsEditor<Config>,
        configuration: Config,
        settings: RunnerAndConfigurationSettings
    ): DialogWrapper {
        return object : DialogWrapper(event.project) {

            init {
                title = event.presentation.text
                init()
            }

            override fun createCenterPanel() = editor.component

            override fun doValidate(): ValidationInfo? {
                editor.applyTo(configuration)
                try {
                    settings.checkSettings()
                } catch (error: RuntimeConfigurationError) {
                    return ValidationInfo(error.message.orEmpty())
                } catch (error: RuntimeConfigurationWarning) {
                    return ValidationInfo(error.message.orEmpty()).asWarning().withOKEnabled()
                }
                return null
            }
        }
    }

    /**
     * Validate the target file of the requested action.
     *
     * @return true if file is of the correct type
     */
    open fun isValidTarget(): Boolean {
        // This is not very reliable because file type detection is brittle.
        // It depends on the user's IDE setup, including any plugins which
        // might hijack the YAML file type.
        // TODO: Each subclass needs to provide its own validation.
        return targetFile.fileType is YAMLFileType
    }

    /**
     * Get a name for the new run configuration.
     *
     * @return configuration name
     */
    protected abstract fun getName(): String

    /**
     * Get a run configuration factory.
     *
     * @return factory instance of the correct type
     */
    protected abstract fun getFactory(): ConfigurationFactory

    /**
     * Get the current run configuration instance.
     *
     * @return run configuration instance of the correct type
     */
    protected abstract fun getConfiguration(): Config

    /**
     * Get the editor instance for the current run configuration.
     *
     * @return editor instance of the correct type
     */
    protected abstract fun getEditor(): Editor

    /**
     * Set default values for the configuration instance.
     *
     * @param settings: new configuration settings
     * @return set default options for the new run configuration
     */
    protected abstract fun setDefaults(settings: RunnerAndConfigurationSettings)
}


/**
 * Create a Galaxy run configuration for a requirements file.
 */
class CreateGalaxyConfiguration: CreateRunConfiguration<GalaxyOptions, GalaxyRunConfiguration, GalaxyEditor>() {

    private lateinit var configuration: GalaxyRunConfiguration
    private lateinit var editor: GalaxyEditor

    override fun setDefaults(settings: RunnerAndConfigurationSettings) {
        configuration = settings.configuration as GalaxyRunConfiguration
        configuration.let {
            // Save a reference to the editor because each call to
            // configurationEditor seems to be a new instance.
            editor = it.configurationEditor
            it.workDir = projectPath.toString()
            it.requirements = targetPath.toString()
        }
    }

    override fun getName() = "Install dependencies from $targetPath"

    override fun getFactory() = GalaxyConfigurationFactory(AnsibleConfigurationType())

    override fun getConfiguration(): GalaxyRunConfiguration = configuration

    override fun getEditor() = editor
}


/**
 * Create a Playbook run configuration for a playbook file.
 */
class CreatePlaybookConfiguration: CreateRunConfiguration<PlaybookOptions, PlaybookRunConfiguration, PlaybookEditor>() {

    private lateinit var configuration: PlaybookRunConfiguration
    private lateinit var editor: PlaybookEditor

    override fun setDefaults(settings: RunnerAndConfigurationSettings) {
        configuration = settings.configuration as PlaybookRunConfiguration
        configuration.let {
            // Save a reference to the editor because each call to
            // configurationEditor seems to be a new instance.
            editor = it.configurationEditor
            it.workDir = projectPath.toString()
            it.playbooks = mutableListOf(targetPath.toString())
        }
    }

    override fun getName() = "Run playbook $targetPath"

    override fun getFactory() = PlaybookConfigurationFactory(AnsibleConfigurationType())

    override fun getConfiguration() = configuration

    override fun getEditor() = editor
}
