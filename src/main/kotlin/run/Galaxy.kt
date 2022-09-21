package dev.mdklatt.idea.ansible.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import dev.mdklatt.idea.common.exec.CommandLine
import dev.mdklatt.idea.common.exec.PosixCommandLine
import org.jdom.Element
import java.util.*
import javax.swing.JComponent
import kotlin.RuntimeException


/**
 * Factory for GalaxyRunConfiguration instances.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#configuration-factory">Configuration Factory</a>
 */
class GalaxyConfigurationFactory internal constructor(type: ConfigurationType) : ConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance.
     */
    override fun createTemplateConfiguration(project: Project) =
            GalaxyRunConfiguration(project, this, "")

    /**
     * The name of the run configuration variant created by this factory.
     *
     * @returns: name
     */
    override fun getName() = "Ansible Galaxy"

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
    override fun getOptionsClass() = GalaxyOptions::class.java
}


/**
 * Handle persistence of run configuration options.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-configurationfactory">Run Configurations Tutorial</a>
 */
class GalaxyOptions : RunConfigurationOptions() {
    internal var uid by string()
    internal var requirements by string()
    internal var deps by property(true)
    internal var rolesDir by string()
    internal var force by property(false)
    internal var command by string("ansible-galaxy")
    internal var rawOpts by string()
    internal var workDir by string()
}


/**
 * Run Configuration for executing <a href="https://docs.ansible.com/ansible/latest/cli/ansible-galaxy.html">ansible-galaxy</a>.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class GalaxyRunConfiguration internal constructor(project: Project, factory: ConfigurationFactory, name: String) :
        RunConfigurationBase<GalaxyOptions>(project, factory, name) {

    // TODO: Why can't options.<property> be used as a delegate for these?

    internal var uid: String
        get() {
            if (options.uid == null) {
                options.uid = UUID.randomUUID().toString()
            }
            return options.uid ?: throw RuntimeException("null UID")
        }
        set(value) {
            options.uid = value
        }

    // TODO: Shouldn't this be a list?
    internal var requirements: String
        get() = options.requirements ?: ""
        set(value) {
            options.requirements = value
        }
    internal var deps: Boolean
        get() = options.deps
        set(value) {
            options.deps = value
        }
    internal var rolesDir: String
        get() = options.rolesDir ?: ""
        set(value) {
            options.rolesDir = value
        }
    internal var force: Boolean
        get() = options.force
        set(value) {
            options.force = value
        }
    internal var command: String
        get() = options.command ?: ""
        set(value) {
            options.command = value.ifBlank { "ansible-playbook" }
        }
    internal var rawOpts: String
        get() = options.rawOpts ?: ""
        set(value) {
            options.rawOpts = value
        }
    internal var workDir: String
        get() = options.workDir ?: ""
        set(value) {
            options.workDir = value
        }

    /**
     * Get the persistent options for this instance.
     */
    override fun getOptions(): GalaxyOptions {
        return super.getOptions() as GalaxyOptions
    }

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
        super.writeExternal(element)
    }

    /**
     * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
     * returned from this method may also implement [com.intellij.execution.impl.CheckableRunConfigurationEditor]. The returned object
     * can also implement [com.intellij.openapi.options.SettingsEditorGroup] if the settings it provides need to be displayed in
     * multiple tabs.
     *
     * @return the settings editor component.
     */
    override fun getConfigurationEditor() = GalaxySettingsEditor()

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
            GalaxyCommandLineState(this, environment)

}


/**
 * Command line process for executing the run configuration.
 *
 * @param config: run configuration
 * @param environment: execution environment
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-run-configuration">Run Configurations Tutorial</a>
 */
class GalaxyCommandLineState internal constructor(private val config: GalaxyRunConfiguration, environment: ExecutionEnvironment) :
        CommandLineState(environment) {
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
        val command = PosixCommandLine(config.command, listOf("install"))
        val options = mutableMapOf(
            "no-deps" to !config.deps,
            "force" to config.force,
            "role-file" to config.requirements.ifEmpty { null },
            "roles-path" to config.rolesDir.ifEmpty { null }
        )
        command.addOptions(options)
        command.addParameters(CommandLine.split(config.rawOpts))
        if (!command.environment.contains("TERM")) {
            command.environment["TERM"] = "xterm-256color"
        }
        if (config.workDir.isNotBlank()) {
            command.withWorkDirectory(config.workDir)
        }
        return KillableColoredProcessHandler(command).also {
            ProcessTerminatedListener.attach(it, environment.project)
        }
    }
}


/**
 * UI component for Galaxy Run Configuration settings.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#settings-editor">Settings Editor</a>
 */
class GalaxySettingsEditor internal constructor() : SettingsEditor<GalaxyRunConfiguration>() {

    private var requirements = ""
    private var deps = false
    private var rolesDir = ""
    private var force = false
    private var command = ""
    private var rawOpts = ""
    private var workDir = ""

    /**
     * Create the widget for this editor.
     *
     * @return UI widget
     */
    override fun createEditor(): JComponent {
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        return panel {
            row("Requirements:") {
                textFieldWithBrowseButton("Requirements File").bindText(::requirements)
                checkBox("Install transitive dependencies").bindSelected(::deps)
            }
            row("Roles directory:") {
                textFieldWithBrowseButton("Requirements File").bindText(::rolesDir)
                checkBox("Overwrite existing roles").bindSelected(::force)
            }
            group("Environment") {
                row("Ansible command:") {
                    textFieldWithBrowseButton("Ansible Command").bindText(::command)
                }
                row("Raw options:") {
                    expandableTextField().bindText(::rawOpts)
                }
                row("Working directory:") {
                    textFieldWithBrowseButton("Working Directory",
                        fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                    ).bindText(::workDir)
                }
            }
        }
    }

    /**
     * Reset editor fields from the configuration state.
     *
     * @param config: run configuration
     */
    override fun resetEditorFrom(config: GalaxyRunConfiguration) {
        // Update bound properties from config value then reset UI.
        config.let {
            requirements = it.requirements
            deps = it.deps
            force = it.force
            rolesDir = it.rolesDir
            command = it.command
            rawOpts = it.rawOpts
            workDir = it.workDir
        }
        (this.component as DialogPanel).reset()
    }

    /**
     * Apply editor fields to the configuration state.
     *
     * @param config: run configuration
     */
    override fun applyEditorTo(config: GalaxyRunConfiguration) {
        // Apply UI to bound properties then update config values.
        (this.component as DialogPanel).apply()
        config.let {
            it.requirements = requirements
            it.deps = deps
            it.force = force
            it.rolesDir = rolesDir
            it.command = command
            it.rawOpts = rawOpts
            it.workDir = workDir
        }
    }
}
