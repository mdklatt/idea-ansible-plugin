package software.mdklatt.idea.ansible.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.CheckBox
import com.intellij.ui.layout.panel
import com.intellij.util.getOrCreate
import org.jdom.Element
import java.lang.RuntimeException
import java.util.*
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField


/**
 * Factory for PlaybookRunConfiguration instances.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#configuration-factory">Configuration Factory</a>
 */
class PlaybookConfigurationFactory internal constructor(type: ConfigurationType) : ConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance.
     */
    override fun createTemplateConfiguration(project: Project) =
            PlaybookRunConfiguration(project, this, "Anible Playbook")

    /**
     * The name of the run configuration variant created by this factory.
     *
     * @return: name
     */
    override fun getName() = "Ansible Playbook"

    /**
     * Run configuration ID used for serialization.
     *
     * @return: unique ID
     */
    override fun getId(): String = this::class.java.simpleName
}


/**
 * Run Configuration for executing <a href="https://docs.ansible.com/ansible/latest/cli/ansible-playbook.html">ansible-playbook</a>.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class PlaybookRunConfiguration internal constructor(project: Project, factory: ConfigurationFactory, name: String) :
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
    override fun getConfigurationEditor() = PlaybookSettingsEditor(project)

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
            PlaybookCommandLineState(this.settings, environment)

    /**
     * Read settings from a JDOM element.
     *
     * This is part of the RunConfiguration persistence API.
     *
     * @param element: input element.
     */
    override fun readExternal(element: Element) {
        super.readExternal(element)
        settings = PlaybookRunSettings(element)
        return
    }

    /**
     * Write settings to a JDOM element.
     *
     * This is part of the RunConfiguration persistence API.

     * @param element: output element.
     */
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        settings.store(element)
        return
    }
}


/**
 * UI component for Playbook Run Configuration settings.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#settings-editor">Settings Editor</a>
 */
class PlaybookSettingsEditor internal constructor(project: Project) : SettingsEditor<PlaybookRunConfiguration>() {

    var playbooks = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Playbooks", "", project,
                FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor())
    }
    var inventory = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Inventory", "", project,
                FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor())
    }
    var host = JTextField("")
    var sudoPrompt = CheckBox("Prompt for sudo password")
    var tags = JTextField("")
    var variables = JTextField("")
    var command = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Ansible Command", "", project,
                FileChooserDescriptorFactory.createSingleFileDescriptor())
    }
    var rawOpts = RawCommandLineEditor()
    var workDir = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Working Directory", "", project,
                FileChooserDescriptorFactory.createSingleFolderDescriptor())
    }

    /**
     * Create the widget for this editor.
     *
     * @return: UI widget
     */
    override fun createEditor(): JComponent {
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        return panel{
            row("Playbooks:") { playbooks() }
            row("Inventory:") { inventory() }
            row("Host:") {
                host()
                sudoPrompt()
            }
            row("Tags:") { tags() }
            row("Extra variables:") { variables() }
            titledRow("Environment") {}
            row("Ansible command:") { command() }
            row("Raw options:") { rawOpts() }
            row("Working directory:") { workDir() }
        }
    }

    /**
     * Reset editor fields from the saved configuration state.
     *
     * @param config: run configuration
     */
    override fun resetEditorFrom(config: PlaybookRunConfiguration) {
        config.apply {
            playbooks.text = if (settings.playbooks.isNotEmpty()) config.settings.playbooks[0] else ""
            inventory.text = if (settings.inventory.isNotEmpty()) config.settings.inventory[0] else ""
            host.text = settings.host
            sudoPrompt.isSelected = settings.sudoPrompt
            tags.text = settings.tags.joinToString(" ")
            variables.text = settings.variables.joinToString(" ")
            command.text = settings.command
            rawOpts.text = settings.rawOpts
            workDir.text = settings.workDir
        }
        return
    }

    /**
     * Apply editor fields to the configuration state.
     *
     * @param config: run configuration
     */
    override fun applyEditorTo(config: PlaybookRunConfiguration) {
        // This apparently gets called for every key press, so performance is
        // critical.
        // TODO: Get file list for playbooks and inventory.
        config.apply {
            // If a new PlaybookRunSettings object isn't created, multiple
            // configurations seem to share the same settings, i.e. the
            // values for one configuration will be copied to multiple
            // configurations on save. This is not apparent until the project
            // is reloaded. TODO: Why does this happen?
            settings = PlaybookRunSettings()  // prevent cross contamination
            settings.playbooks = listOf(playbooks.text)
            settings.inventory = listOf(inventory.text)
            settings.host = host.text
            settings.sudoPrompt = sudoPrompt.isSelected
            settings.tags = tags.text.split(" ")
            settings.variables = variables.text.split(" ")
            settings.rawOpts = rawOpts.text
            settings.workDir = workDir.text
        }
        return
    }
}


/**
 * TODO
 */
class PlaybookCommandLineState internal constructor(private val settings: PlaybookRunSettings, environment: ExecutionEnvironment) :
        CommandLineState(environment) {

    /**
     * Start the process.
     *
     * @return the handler for the running process
     * @throws ExecutionException if the execution failed.
     * @see GeneralCommandLine
     *
     * @see com.intellij.execution.process.OSProcessHandler
     */
    override fun startProcess(): ProcessHandler {
        val command = PosixCommandLine(settings.command)
        val options = mutableMapOf<String, Any?>(
            "limit" to settings.host.ifEmpty { null },
            "inventory" to settings.inventory.joinToString(",").ifEmpty { null },
            "tags" to settings.tags.joinToString(",").ifEmpty { null },
            "extra-vars" to settings.variables.joinToString(" ").ifEmpty { null }
        )
        if (settings.sudoPrompt) {
            val dialog = PasswordDialog("Sudo password for ${settings.host}")
            val password = dialog.prompt() ?: throw RuntimeException("no password")
            options["ask-become-pass"] = true
            command.withInput(password.joinToString(""))
            password.fill('\u0000')  // overwrite value
        }
        command.addOptions(options)
        command.addParameters(PosixCommandLine.split(settings.rawOpts))
        command.addParameters(settings.playbooks)
        if (!command.environment.contains("TERM")) {
            command.environment["TERM"] = "xterm-256color"
        }
        if (settings.workDir.isNotBlank()) {
            command.withWorkDirectory(settings.workDir)
        }
        val process = KillableColoredProcessHandler(command)
        ProcessTerminatedListener.attach(process, environment.project)
        return process
    }
}


/**
 * Manage PlaybookRunConfiguration runtime settings.
 */
class PlaybookRunSettings internal constructor() {

    companion object {
        private const val DELIMIT = "|"
        private const val JDOM_TAG = "ansible-playbook"
    }

    private var id: UUID? = null

    var playbooks = emptyList<String>()
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }
    var inventory = emptyList<String>()
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }
    var host = ""
    var sudoPrompt = false
    var tags = emptyList<String>()
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }
    var variables = emptyList<String>()
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }
    var command = ""
        get() = field.ifEmpty { "ansible-playbook" }
    var rawOpts = ""
    var workDir = ""


    /**
     * Construct object from a JDOM element.
     *
     * @param element: input element
     */
    internal constructor(element: Element) : this() {
        element.getOrCreate(JDOM_TAG).let {
            val str = JDOMExternalizerUtil.readField(it, "id", "")
            id = if (str.isEmpty()) UUID.randomUUID() else UUID.fromString(str)
            playbooks = JDOMExternalizerUtil.readField(it, "playbooks", "").split(DELIMIT)
            inventory = JDOMExternalizerUtil.readField(it, "inventory", "").split(DELIMIT)
            host = JDOMExternalizerUtil.readField(it, "host", "")
            sudoPrompt = JDOMExternalizerUtil.readField(it, "sudoPrompt", "false").toBoolean()
            tags = JDOMExternalizerUtil.readField(it, "tags", "").split(DELIMIT)
            variables = JDOMExternalizerUtil.readField(it, "variables", "").split(DELIMIT)
            command = JDOMExternalizerUtil.readField(it, "command", "")
            rawOpts = JDOMExternalizerUtil.readField(it, "rawOpts", "")
            workDir = JDOMExternalizerUtil.readField(it, "workDir", "")
        }
        return
    }

    /**
     * Store settings.
     *
     * @param element: JDOM element
     */
    fun store(element: Element) {
        val default = element.getAttributeValue("default")?.toBoolean() ?: false
         element.getOrCreate(JDOM_TAG).let {
            if (!default) {
                // Don't save an ID with the config template.
                if (id == null) {
                    id = UUID.randomUUID()
                }
                JDOMExternalizerUtil.writeField(it, "id", id.toString())
            }
            JDOMExternalizerUtil.writeField(it, "playbooks", playbooks.joinToString(DELIMIT))
            JDOMExternalizerUtil.writeField(it, "inventory", inventory.joinToString(DELIMIT))
            JDOMExternalizerUtil.writeField(it, "host", host)
            JDOMExternalizerUtil.writeField(it, "sudoPrompt", sudoPrompt.toString())
            JDOMExternalizerUtil.writeField(it, "tags", tags.joinToString(DELIMIT))
            JDOMExternalizerUtil.writeField(it, "variables", variables.joinToString(DELIMIT))
            JDOMExternalizerUtil.writeField(it, "command", command)
            JDOMExternalizerUtil.writeField(it, "rawOpts", rawOpts)
            JDOMExternalizerUtil.writeField(it, "workDir", workDir)
        }
        return
    }
}


/**
 * Modal dialog for a password prompt.
 */
private class PasswordDialog(private val prompt: String ="Password") : DialogWrapper(false) {

    private var field = JPasswordField("", 20)
    private var value = charArrayOf()

    init {
        init()
        title = "Password"
    }

    /**
     * Prompt the user for the password.
     *
     * @return user input
     */
    fun prompt(): CharArray? = if (showAndGet()) value else null

    /**
     * Define dialog contents.
     *
     * @return: dialog contents
     */
    protected override fun createCenterPanel(): JComponent {
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        return panel{
            row("${prompt}:") { field() }
        }
    }

    /**
     * Event handler for the OK button.
     */
    protected override fun doOKAction() {
        value = field.password
        super.doOKAction()
    }
}