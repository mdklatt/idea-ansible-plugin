package software.mdklatt.idea.ansible.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.DefaultProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.layout.panel
import javax.swing.JComponent
import javax.swing.JTextField


/**
 * TODO
 */
class PlaybookConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance.
     */
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return PlaybookRunConfiguration(project, this, "Ansible Playbook")
    }
}


/**
 * TODO
 */
class PlaybookRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
        RunConfigurationBase<RunProfileState>(project, factory, name) {

    var settings = PlaybookRunSettings()

    /**
     * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
     * returned from this method may also implement [com.intellij.execution.impl.CheckableRunConfigurationEditor]. The returned object
     * can also implement [com.intellij.openapi.options.SettingsEditorGroup] if the settings it provides need to be displayed in
     * multiple tabs.
     *
     * @return the settings editor component.
     */
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PlaybookSettingsEditor(project)
    }

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        return PlaybookCommandLineState(this, environment)
    }

}


/**
 * TODO
 */
class PlaybookSettingsEditor(project: Project) : SettingsEditor<PlaybookRunConfiguration>() {

    var playbooks = TextFieldWithBrowseButton()
    var inventory = TextFieldWithBrowseButton()
    var host = JTextField("")
    var tags = JTextField("")
    var variables = JTextField("")
    var options = RawCommandLineEditor()
    var command = TextFieldWithBrowseButton()
    var workdir = TextFieldWithBrowseButton()

    init {
        val fileChooser = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor()
        playbooks.addBrowseFolderListener("Playbooks", "", project, fileChooser)
        inventory.addBrowseFolderListener("Inventory", "", project, fileChooser)
        command.addBrowseFolderListener("Playbook Command", "", project, fileChooser)
        val dirChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        workdir.addBrowseFolderListener("Working Directory", "", project, dirChooser)
    }

    private var settingsPanel = panel{
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        row("Playbooks:") { playbooks() }
        row("Inventory:") { inventory() }
        row("Host:") { host() }
        row("Tags:") { tags() }
        row("Extra variables:") { variables() }
        row("Raw options:") { options() }
        titledRow("Ansible Settings") {}
        row("Playbook command:") { command() }
        row("Working directory:") { workdir() }
    }

    /**
     * Reset editor fields from the saved configuration state.
     *
     * @param config: run configuration
     */
    override fun resetEditorFrom(config: PlaybookRunConfiguration) {
        playbooks.text = if (config.settings.playbooks.isNotEmpty()) config.settings.playbooks[0] else ""
        inventory.text = if (config.settings.inventory.isNotEmpty()) config.settings.inventory[0] else ""
        host.text = config.settings.host
        tags.text = config.settings.tags.joinToString(" ")
        variables.text = config.settings.variables.joinToString(" ")
        command.text = config.settings.command
        options.text = config.settings.options.joinToString("")
        workdir.text = config.settings.workdir
        return
    }

    override fun createEditor(): JComponent {
        return settingsPanel
    }

    override fun applyEditorTo(config: PlaybookRunConfiguration) {
        // This apparently gets called for every key press, so performance is
        // critical.
        // TODO: Get file list for playbooks and inventory.
        config.settings.playbooks = listOf(playbooks.text)
        config.settings.inventory = listOf(inventory.text)
        config.settings.host = host.text
        config.settings.tags = if (tags.text.isNotBlank()) tags.text.split(" ") else emptyList()
        config.settings.variables = if (variables.text.isNotBlank()) variables.text.split(" ") else emptyList()
        config.settings.options = if (options.text.isNotBlank()) options.text.split(" ") else emptyList()
        config.settings.workdir = workdir.text
        return
    }
}


/**
 * TODO
 */
class PlaybookRunner : DefaultProgramRunner() {  // FIXME: deprecation
    /**
     * Checks if the program runner is capable of running the specified configuration with the specified executor.
     *
     * @param executorId ID of the [Executor] with which the user is trying to run the configuration.
     * @param profile the configuration being run.
     * @return true if the runner can handle it, false otherwise.
     */
    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return DefaultRunExecutor.EXECUTOR_ID == executorId && profile is PlaybookRunConfiguration
    }

    /**
     * Returns the unique ID of this runner. This ID is used to store settings and must not change between plugin versions.
     *
     * @return the program runner ID.
     */
    override fun getRunnerId(): String {
        return "AnsiblePlaybookRunner"
    }
}


/**
 * TODO
 */
class PlaybookCommandLineState(private val config: PlaybookRunConfiguration, environment: ExecutionEnvironment) : CommandLineState(environment) {
    /**
     * Starts the process.
     *
     * @return the handler for the running process
     * @throws ExecutionException if the execution failed.
     * @see GeneralCommandLine
     *
     * @see com.intellij.execution.process.OSProcessHandler
     */
    override fun startProcess(): ProcessHandler {
        fun nullBlank(str: String): String? {
            return if (str.isNotBlank()) str else null
        }
        val settings = config.settings
        val command = PosixCommandLine(settings.command)
        val options = mutableMapOf<String, Any?>(
            "verbose" to true,  // TODO: user option
            "limit" to nullBlank(settings.host),
            "inventory" to nullBlank(settings.inventory.joinToString(",")),
            "tags" to nullBlank(settings.tags.joinToString(",")),
            "extra-vars" to nullBlank(settings.variables.joinToString(" "))
        )
        command.addOptions(options)
        command.addParameters(config.settings.options)
        command.addParameters(settings.playbooks)
        if (!command.environment.contains("TERM")) {
            command.environment["TERM"] = "xterm-256color"
        }
        if (settings.workdir.isNotBlank()) {
            command.withWorkDirectory(settings.workdir)
        }
        val process = KillableColoredProcessHandler(command)
        ProcessTerminatedListener.attach(process, environment.project)
        return process
    }
}


/**
 * Manage PlaybookRunConfiguration runtime settings.
 */
class PlaybookRunSettings {
    var playbooks = emptyList<String>()
    var inventory = emptyList<String>()
    var host = ""
    var tags = emptyList<String>()  // TODO: Set
    var variables = emptyList<String>()  // TODO: Set
    var command = ""
        get() = if (field.isNotBlank()) field else "ansible-playbook"
    var options = emptyList<String>()
    var workdir = ""
}
