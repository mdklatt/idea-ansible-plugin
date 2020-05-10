package software.mdklatt.idea.ansible.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.DefaultProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.layout.panel
import javax.swing.JComponent
import javax.swing.JPasswordField
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
        return PlaybookSettingsEditor()
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
class PlaybookSettingsEditor : SettingsEditor<PlaybookRunConfiguration>() {

    // FIXME: Browse buttons are non-functional--addBrowseFolderListener()?
    var playbooks = TextFieldWithBrowseButton()
    var inventory = TextFieldWithBrowseButton()
    var host = JTextField("")
    var sudo = JPasswordField("")  // FIXME: https://www.jetbrains.org/intellij/sdk/docs/basics/persisting_sensitive_data.html
    var tags = JTextField("")
    var workdir = TextFieldWithBrowseButton()

    private var settingsPanel = panel{
        // Kotlin UI DSL: https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        row("Playbooks:") { playbooks() }
        row("Inventory:") { inventory() }
        row("Host:") { host() }
        row("Tags:") { tags() }
        row("Sudo password:") { sudo() }
        row("Working directory:") { workdir() }
    }

    override fun resetEditorFrom(config: PlaybookRunConfiguration) {
        playbooks.text = if (config.settings.playbooks.isNotEmpty()) config.settings.playbooks[0] else ""
        host.text = config.settings.host
        sudo.text = config.settings.sudo
        tags.text = config.settings.tags.joinToString(" ")
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
        config.settings.sudo = sudo.password.toString()
        config.settings.tags = tags.text.split(" ")
        config.settings.workdir = workdir.text
        return
    }
}


/**
 * TODO
 */
class PlaybookRunner : DefaultProgramRunner() {
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
class PlaybookCommandLineState(val config: PlaybookRunConfiguration, environment: ExecutionEnvironment) : CommandLineState(environment) {
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
        val settings = config.settings
        val command = PosixCommandLine("ansible-playbook")
        val options = mutableMapOf<String, Any>(
            "verbose" to true,  // TODO: user option
            "limit" to settings.host,
            "inventory" to settings.inventory.joinToString(","),
            "tags" to settings.tags.joinToString(",")
        )
        if (settings.sudo.isNotBlank()) {
            options["ask-become-pass"] = true
            command.withInput(settings.workdir)
        }
        command.addOptions(options)
        command.addParameters(settings.playbooks)
        if (!command.environment.containsKey("TERM")) {
            command.environment["TERM"] = "xterm-256color"
        }
        command.withWorkDirectory(settings.workdir)
        val process = KillableColoredProcessHandler(command)
        ProcessTerminatedListener.attach(process, environment.project)
        return process
    }
}

/**
 * Manage PlaybookRunConfiguration runtime settings.
 */
class PlaybookRunSettings {
    // TODO: Could just be a Map<String, Any>.
    var playbooks = emptyList<String>()
    var inventory = emptyList<String>()
    var host = ""
    var sudo = ""
    var tags = emptyList<String>()
    var workdir = ""
}
