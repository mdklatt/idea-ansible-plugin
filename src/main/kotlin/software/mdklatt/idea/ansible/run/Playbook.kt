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


class PlaybookRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
        RunConfigurationBase<RunProfileState>(project, factory, name) {

    var host: String? = null
    var sudo: String? = null
    var tags = listOf<String>()

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
        return PlaybookCommandLineState(environment)
    }

}


class PlaybookSettingsEditor : SettingsEditor<PlaybookRunConfiguration>() {

    var playbooks = TextFieldWithBrowseButton()
    var host = JTextField()
    var sudo = JPasswordField("")
    var tags = JTextField()

    private var settingsPanel = panel{
        // Kotlin UI DSL: https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        row("Playbooks:") { playbooks() }
        row("Host:") { host() }
        row("Sudo password:") { sudo() }
        row("Tags:") { tags() }
    }

    override fun resetEditorFrom(config: PlaybookRunConfiguration) {
        // TODO: playbooks.addBrowseFolderListener()
        host.text = config.host
        sudo.text = config.sudo
        tags.text = config.tags.joinToString(" ")
        return
    }

    override fun createEditor(): JComponent {
        return settingsPanel
    }

    override fun applyEditorTo(config: PlaybookRunConfiguration) {
        // This apparently gets called for every key press.
        config.host = host.text
        config.sudo = sudo.password.toString()
        config.tags = tags.text.split(" ")
        return
    }
}


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


class PlaybookCommandLineState(environment: ExecutionEnvironment) : CommandLineState(environment) {
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
        val cmdl = GeneralCommandLine("ansible-playbook", "--version")
        if (!cmdl.environment.containsKey("TERM")) {
            cmdl.environment["TERM"] = "xterm-256color"
        }
        val process = KillableColoredProcessHandler(cmdl)
        ProcessTerminatedListener.attach(process, environment.project)
        return process
    }
}
