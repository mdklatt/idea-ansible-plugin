/**
 * Playbook run configuration for executing `ansible-playbook`.
 *
 * <https://docs.ansible.com/ansible/latest/cli/ansible-playbook.html>
 */
package dev.mdklatt.idea.ansible.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.*
import dev.mdklatt.idea.common.exec.CommandLine
import dev.mdklatt.idea.common.exec.PosixCommandLine
import dev.mdklatt.idea.common.password.PasswordDialog
import dev.mdklatt.idea.common.password.StoredPassword
import org.jdom.Element
import java.awt.event.ItemEvent
import java.io.File
import java.util.*
import javax.swing.JPasswordField


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

    /**
     * Return the type of the options storage class.
     *
     * @return: options class type
     */
    override fun getOptionsClass() = PlaybookOptions::class.java
}


/**
 * Handle persistence of run configuration options.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-configurationfactory">Run Configurations Tutorial</a>
 */
class PlaybookOptions : AnsibleOptions() {
    internal var playbooks by string()
    internal var inventory by string()
    internal var host by string()
    internal var sudoPassPrompt by property(false)
    internal var tags by string()
    internal var variables by string()
}


/**
 * Run Configuration for executing <a href="https://docs.ansible.com/ansible/latest/cli/ansible-playbook.html">ansible-playbook</a>.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class PlaybookRunConfiguration internal constructor(project: Project, factory: ConfigurationFactory, name: String) :
    AnsibleRunConfiguration<PlaybookOptions>(project, factory, name, "ansible-playbook") {

    private val delimit = "|"

    internal var playbooks: MutableList<String>
        get() = options.playbooks?.split(File.pathSeparator)?.toMutableList() ?: mutableListOf()
        set(value) {
            options.playbooks = value.joinToString(File.pathSeparator)
        }
    internal var inventory: MutableList<String>
        get() = options.inventory?.split(File.pathSeparator)?.toMutableList() ?: mutableListOf()
        set(value) {
            options.inventory = value.joinToString(File.pathSeparator)
        }
    internal var host: String
        get() = options.host ?: ""
        set(value) {
            options.host = value
        }
    internal val sudoPass: StoredPassword
        get() = StoredPassword(uid)  // need password for current UID
    internal var sudoPassPrompt: Boolean
        get() = options.sudoPassPrompt
        set(value) {
            options.sudoPassPrompt = value
        }
    internal var tags: List<String>
        get() = options.tags?.split(delimit) ?: emptyList()
        set(value) {
            options.tags = value.joinToString(delimit)
        }
    internal var variables: List<String>
        get() = options.variables?.split(delimit) ?: emptyList()
        set(value) {
            options.variables = value.joinToString(delimit)
        }

    /**
     * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
     * returned from this method may also implement [com.intellij.execution.impl.CheckableRunConfigurationEditor]. The returned object
     * can also implement [com.intellij.openapi.options.SettingsEditorGroup] if the settings it provides need to be displayed in
     * multiple tabs.
     *
     * @return the settings editor component.
     */
    override fun getConfigurationEditor() = PlaybookEditor()

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
        PlaybookCommandLineState(environment)

    /**
     * Read stored settings from XML.
     *
     * @param element XML element
     */
    override fun readExternal(element: Element) {
        super.readExternal(element)
        if (options.uid == null) {
            options.uid = UUID.randomUUID().toString()
        }
    }

    /**
     * Write stored settings to XML.
     *
     * @param element XML element
     */
    override fun writeExternal(element: Element) {
        val default = element.getAttributeValue("default")?.toBoolean() ?: false
        if (default) {
            // Do not save UID with configuration template.
            options.uid = null
        }
        if (sudoPassPrompt) {
            // Do not use saved password.
            sudoPass.value = null
        }
        super.writeExternal(element)
    }
}


/**
 * UI component for Playbook Run Configuration settings.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#settings-editor">Settings Editor</a>
 */
class PlaybookEditor internal constructor() : AnsibleEditor<PlaybookOptions, PlaybookRunConfiguration>() {

    private var playbooks = mutableListOf<String>()
    private var inventory = mutableListOf<String>()
    private var host = ""
    private var sudoPass = charArrayOf()
    private var sudoPassPrompt = false
    private var tags = ""
    private var variables = ""

    /**
     * Add command-specific settings to the UI component.
     *
     * @param parent: parent component builder
     */
    override fun addCommandFields(parent: Panel) {
        // Multiple file selection does not work as expected because the UI
        // widget does not allow multiple items to be selected. The way that
        // multiple paths are stored is unknown without a working example, but
        // it's reasonable to assume that a pathsep-delimited string is used.
        val getPathsFromField: (TextFieldWithBrowseButton) -> MutableList<String> = {
                field -> field.text.split(File.pathSeparator).toMutableList()
        }
        val setFieldFromPaths: (TextFieldWithBrowseButton, List<String>) -> Unit = {
                field, paths -> field.text = paths.joinToString(File.pathSeparator)
        }
        parent.let {
            it.row("Playbooks:") {
                // FIXME: Multiple file selection does not work.
                textFieldWithBrowseButton("Playbooks",
                    fileChooserDescriptor = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor(),
                ).bind(getPathsFromField, setFieldFromPaths, ::playbooks.toMutableProperty())
            }
            it.row("Inventories:") {
                // FIXME: Multiple file selection does not work.
                textFieldWithBrowseButton("Inventories",
                    fileChooserDescriptor = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor(),
                ).bind(getPathsFromField, setFieldFromPaths, ::inventory.toMutableProperty())
            }
            it.row("Host specification:")  {
                textField().bindText(::host)
            }
            it.row("Host sudo password:") {
                val password = JPasswordField("", 20)
                cell(password).applyIfEnabled().bind(
                    JPasswordField::getPassword,
                    { field, value -> field.text = value.joinToString("") },
                    ::sudoPass.toMutableProperty()
                )
                checkBox("Prompt for password").let {
                    it.bindSelected(::sudoPassPrompt)
                    it.component.addItemListener{ event ->
                        // Selecting this checkbox disables the password field,
                        // and the user will instead be prompted for a password
                        // at runtime.
                        if (event.stateChange == ItemEvent.SELECTED) {
                            password.text = ""
                            password.isEnabled = false
                        }
                        else {
                            password.isEnabled = true
                        }
                    }
                }
            }
            it.row("Tags:")  {
                textField().bindText(::tags)
            }
            it.row("Extra variables:")  {
                textField().bindText(::variables)
            }
        }
    }

    /**
     * Reset UI with command options from configuration.
     *
     * @param config: run configuration
     */
    override fun resetCommandOptions(config: PlaybookRunConfiguration) {
        config.let {
            // TODO: Handle multiple files for playbooks and inventory.
            playbooks = it.playbooks
            inventory = it.inventory
            host = it.host
            sudoPass = it.sudoPass.value ?: charArrayOf()
            sudoPassPrompt = it.sudoPassPrompt
            tags = it.tags.joinToString(" ")
            variables = it.variables.joinToString(" ")
        }
    }

    /**
     * Apply command options from UI to configuration.
     *
     * @param config: run configuration
     */
    override fun applyCommandOptions(config: PlaybookRunConfiguration) {
        config.let {
            // TODO: Handle multiple files for playbooks and inventory.
            it.playbooks = playbooks
            it.inventory = inventory
            it.host = host
            it.sudoPass.value = sudoPass
            it.sudoPassPrompt = sudoPassPrompt
            it.tags = tags.split(" ")
            it.variables = variables.split(" ")
        }
        return
    }
}


/**
 * Command line process for executing the run configuration.
 *
 * @param environment: execution environment
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-run-configuration">Run Configurations Tutorial</a>
 */
class PlaybookCommandLineState internal constructor(environment: ExecutionEnvironment) :
        AnsibleCommandLineState(environment) {

    private val config = environment.runnerAndConfigurationSettings?.configuration as PlaybookRunConfiguration

    /**
     * Get command to execute.
     *
     * @return ansible-playbook command
     */
    override fun getCommand(): PosixCommandLine {
        val options = mutableMapOf<String, Any?>(
            "limit" to config.host.ifEmpty { null },
            "inventory" to config.inventory.joinToString(",").ifEmpty { null },
            "tags" to config.tags.joinToString(",").ifEmpty { null },
            "extra-vars" to config.variables.joinToString(" ").ifEmpty { null },
        )
        val command = ansibleSettings.resolveAnsiblePath(config.ansibleCommand)
        return PosixCommandLine(command).also {
            getPassword()?.let { password ->
                it.withInput(password)
                options["ask-become-pass"] = true
            }
            it.addOptions(options)
            it.addParameters(CommandLine.splitArguments(config.rawOpts))
            it.addParameters(config.playbooks)
            if (config.workDir.isNotBlank()) {
                it.withWorkDirectory(config.workDir)
            }
        }
    }

    /**
     * Get host password.
     *
     * @return password or null if not applicable
     */
    private fun getPassword(): CharArray? {
        return if (config.sudoPassPrompt) {
            // Prompt user for password.
            val dialog = PasswordDialog("Host Password", "Password for host")
            dialog.getPassword() ?: throw RuntimeException("no password")
        } else {
            // Check for stored password.
            config.sudoPass.value
        }
    }
}
