package dev.mdklatt.idea.ansible.actions

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.configurations.RuntimeConfigurationWarning
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import dev.mdklatt.idea.ansible.run.AnsibleConfigurationType
import dev.mdklatt.idea.ansible.run.PlaybookConfigurationFactory
import dev.mdklatt.idea.ansible.run.PlaybookRunConfiguration
import org.jetbrains.yaml.YAMLFileType
import kotlin.io.path.Path
import kotlin.io.path.relativeTo


/**
 *
 */
class CreatePlaybookConfiguration: AnAction() {

    private val logger = Logger.getInstance(this::class.java)  // runtime class resolution

    /**
     * Execute the action.
     *
     * @param event Carries information on the invocation place
     */
    override fun actionPerformed(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.PSI_FILE)?.containingFile
        val path = file?.virtualFile?.canonicalPath
        if (file?.fileType is YAMLFileType && path != null) {
            // TODO: Need better testing that this is a playbook.
            createConfiguration(event.project, Path(path))
        } else {
            Messages.showMessageDialog(
                event.project,
                "Not a valid Playbook file",
                event.presentation.text,
                Messages.getErrorIcon()
            )
        }
    }

    /**
     * Create a new run configuration from a dialog.
     *
     * @param project: parent project
     * @param yamlPath: selected YAML file
     */
    private fun createConfiguration(project: Project?, yamlPath: java.nio.file.Path) {
        // TODO: Refactor in smaller, testable methods.
        val runManager = RunManager.getInstance(project ?: throw RuntimeException("null project"))
        val factory = PlaybookConfigurationFactory(AnsibleConfigurationType())
        val workDir = Path(project.basePath.orEmpty())
        val playbookPath = yamlPath.relativeTo(workDir)
        val name = "Run playbook $playbookPath"
        val configuration = runManager.createConfiguration(name, factory)
        val runConfiguration = configuration.configuration as PlaybookRunConfiguration
        runConfiguration.workDir = project.basePath.orEmpty()
        runConfiguration.playbooks = mutableListOf(playbookPath.toString())
        val editor = runConfiguration.configurationEditor
        editor.resetFrom(runConfiguration)
        val dialog = object : DialogWrapper(project) {
            init {
                title = "Create Playbook Run/Debug Configuration"
                init()
            }

            override fun createCenterPanel() = editor.component

            override fun doValidate(): ValidationInfo? {
                editor.applyTo(runConfiguration)
                try {
                    configuration.checkSettings()
                } catch (error: RuntimeConfigurationError) {
                    return ValidationInfo(error.message.orEmpty())
                } catch (error: RuntimeConfigurationWarning) {
                    return ValidationInfo(error.message.orEmpty()).asWarning().withOKEnabled()
                }
                return null
            }
        }

        if (dialog.showAndGet()) {
            editor.applyTo(runConfiguration)
            runManager.setUniqueNameIfNeeded(configuration)
            logger.info("Created run configuration ${configuration.name}")
            runManager.addConfiguration(configuration)
            runManager.selectedConfiguration = configuration
        }
    }
}
