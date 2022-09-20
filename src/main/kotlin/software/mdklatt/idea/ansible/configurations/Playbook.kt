package software.mdklatt.idea.ansible.configurations

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.CheckBox
import com.intellij.ui.layout.panel
import com.intellij.util.getOrCreate
import dev.mdklatt.idea.common.exec.CommandLine
import dev.mdklatt.idea.common.exec.PosixCommandLine
import dev.mdklatt.idea.common.password.PasswordDialog
import dev.mdklatt.idea.common.password.StoredPassword
import org.jdom.Element
import java.awt.event.ItemEvent
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
            PlaybookRunConfiguration(project, this, "Ansible Playbook")

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

    internal var settings = PlaybookSettings()

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
        settings.load(element)
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
        settings.save(element)
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
    var password = JPasswordField("")
    var passwordPrompt = CheckBox("Prompt for password")
    var tags = JTextField("")
    var variables = JTextField("")

    // Common Ansible settings.
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
        passwordPrompt.addItemListener{
            if (it.stateChange == ItemEvent.SELECTED) {
                password.text = ""
                password.isEditable = false
            }
            else {
                password.isEditable = true
            }
        }
        return panel{
            row("Playbooks:") { playbooks() }
            row("Inventory:") { inventory() }
            row("Host:")  { host() }
            row("Sudo password:") {
                password()
                passwordPrompt()
            }
            row("Tags:") { tags() }
            row("Extra variables:") { variables() }

            // Common Ansible settings.
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
            playbooks.text = if (settings.playbooks.isNotEmpty()) settings.playbooks[0] else ""
            inventory.text = if (settings.inventory.isNotEmpty()) settings.inventory[0] else ""
            host.text = settings.host
            passwordPrompt.isSelected = settings.sudoPrompt
            if (!passwordPrompt.isSelected) {
                password.text = settings.sudoPass.joinToString("")
            }
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
            // configurations seem to share the same settings, i.e. the values
            // for one configuration will be copied to multiple configurations
            // on save. This is not apparent until the project is reloaded.
            // TODO: Why does this happen?
            settings = PlaybookSettings()  // prevent cross contamination
            settings.playbooks = listOf(playbooks.text)
            settings.inventory = listOf(inventory.text)
            settings.host = host.text
            settings.sudoPrompt = passwordPrompt.isSelected
            if (!settings.sudoPrompt) {
                settings.sudoPass = password.password
            }
            settings.tags = tags.text.split(" ")
            settings.variables = variables.text.split(" ")

            // Common Ansible settings.
            settings.command = command.text
            settings.rawOpts = rawOpts.text
            settings.workDir = workDir.text
        }
        return
    }
}


/**
 * TODO
 */
class PlaybookCommandLineState internal constructor(private val settings: PlaybookSettings, environment: ExecutionEnvironment) :
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
            settings.sudoPass = dialog.getPassword() ?: throw RuntimeException("no password")
        }
        if (settings.sudoPass.isNotEmpty()) {
            options["ask-become-pass"] = true
            command.withInput(settings.sudoPass)
        }
        command.addOptions(options)
        command.addParameters(CommandLine.split(settings.rawOpts))
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
 * Manage PlaybookRunConfiguration settings.
 */
internal class PlaybookSettings: AnsibleSettings() {

    override val commandName = "ansible-playbook"
    override val xmlTagName = "ansible-playbook"

    private val delimit =  "|"

    var playbooks = emptyList<String>()
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }
    var inventory = emptyList<String>()
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }
    var host = ""
    var sudoPass = charArrayOf()
    var sudoPrompt = false
    var tags = emptyList<String>()
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }
    var variables = emptyList<String>()
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }

    /**
     * Load stored settings.
     *
     * @param element:
     */
    internal override fun load(element: Element) {
        super.load(element)
        element.getOrCreate(xmlTagName).let {
            playbooks = JDOMExternalizerUtil.readField(it, "playbooks", "").split(delimit)
            inventory = JDOMExternalizerUtil.readField(it, "inventory", "").split(delimit)
            host = JDOMExternalizerUtil.readField(it, "host", "")
            sudoPrompt = JDOMExternalizerUtil.readField(it, "sudoPrompt", "false").toBoolean()
            tags = JDOMExternalizerUtil.readField(it, "tags", "").split(delimit)
            variables = JDOMExternalizerUtil.readField(it, "variables", "").split(delimit)
        }
        sudoPass = StoredPassword(id.toString()).value ?: charArrayOf()
        return
    }

    /**
     * Save settings.
     *
     * @param element: JDOM element
     */
    internal override fun save(element: Element) {
        super.save(element)
        element.getOrCreate(xmlTagName).let {
            JDOMExternalizerUtil.writeField(it, "playbooks", playbooks.joinToString(delimit))
            JDOMExternalizerUtil.writeField(it, "inventory", inventory.joinToString(delimit))
            JDOMExternalizerUtil.writeField(it, "host", host)
            JDOMExternalizerUtil.writeField(it, "sudoPrompt", sudoPrompt.toString())
            JDOMExternalizerUtil.writeField(it, "tags", tags.joinToString(delimit))
            JDOMExternalizerUtil.writeField(it, "variables", variables.joinToString(delimit))
        }

        val storedPassword = StoredPassword(id.toString())
        if (sudoPass.isNotEmpty()) {
            storedPassword.value = sudoPass
        }
        else {
            storedPassword.value = null  // remove from credential store
        }
        return
    }
}