package dev.mdklatt.idea.ansible.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import dev.mdklatt.idea.common.exec.CommandLine
import dev.mdklatt.idea.common.exec.PosixCommandLine
import javax.swing.JComponent
import kotlin.io.path.Path
import kotlin.io.path.pathString


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
class GalaxyOptions : AnsibleOptions("ansible-galaxy") {
    internal var requirements by string()
    internal var deps by property(true)
    internal var collectionsDir by string()
    internal var rolesDir by string()
    internal var force by property(false)
}


/**
 * Run Configuration for executing <a href="https://docs.ansible.com/ansible/latest/cli/ansible-galaxy.html">ansible-galaxy</a>.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class GalaxyRunConfiguration internal constructor(project: Project, factory: ConfigurationFactory, name: String) :
        AnsibleRunConfiguration<GalaxyOptions>(project, factory, name, "ansible-galaxy") {

    // TODO: Why can't options.<property> be used as a delegate for these?

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
    internal var collectionsDir: String
        get() = options.collectionsDir ?: ""
        set(value) {
            options.collectionsDir = value
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
            GalaxyCommandLineState(environment)

}


/**
 * Command line process for executing the run configuration.
 *
 * @param environment: execution environment
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-run-configuration">Run Configurations Tutorial</a>
 */
class GalaxyCommandLineState internal constructor(environment: ExecutionEnvironment) :
        AnsibleCommandLineState(environment) {

    private val config = environment.runnerAndConfigurationSettings?.configuration as GalaxyRunConfiguration

    /**
     * Get command to execute.
     *
     * @return ansible-galaxy command
     */
    override fun getCommand(): PosixCommandLine {
        // Per Ansible docs, if any custom install directory is used, the
        // specific `ansible role install` and `ansible collection install`
        // commands need to be used instead of the general `ansible install`.
        // The preferred way to run multiple processes would be to do it
        // programmatically, but the CommandLineState base class is designed to
        // execute a single process. The `execute()` and/or `createProcess()`
        // methods could be overridden to get around this, but such cleverness
        // is definitely a code small. Instead, multiple execution is being
        // delegated to the shell. Ansible requires WSL on Windows anyway, so
        // this is technically OS-agnostic.
        val rolesDir = config.rolesDir.ifEmpty { null }
        val collectionsDir = config.collectionsDir.ifEmpty { null }
        val command = if (rolesDir == null && collectionsDir == null) {
            // Use basic all-in-one command.
            createCommand()
        }
        else {
            // Need to install collections and roles separately because
            // non-default output directories were specified.
            joinCommands(
                createCommand("collection", collectionsDir),
                createCommand("role", rolesDir),
            )
        }
        return command.also {
            if (config.virtualEnv.isNotBlank()) {
                val path = Path(config.workDir, config.virtualEnv)
                it.withPythonVenv(path.pathString)
            }
            if (config.workDir.isNotBlank()) {
                it.withWorkDirectory(config.workDir)
            }
        }
    }

    /**
     * Construct an install command.
     *
     * @param type: installation type (default, 'collection', or 'role')
     * @param path: installation path
     * @return installation command
     */
    private fun createCommand(type: String? = null, path: String? = null): PosixCommandLine {
        // An Ansible control node must be *nix, so PosixCommandLine is the
        // OS-agnostic choice here.
        // https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html#control-node-requirements
        val forceOption = if (config.deps) "force-with-deps" else "force"
        val commonOptions = mapOf(
            "no-deps" to !config.deps,
            forceOption to config.force,
            "r" to config.requirements.ifEmpty { null },
        )
        val subcommand = sequenceOf(type, "install").filterNotNull()
        return PosixCommandLine(config.command, subcommand).also {
            it.addOptions(commonOptions + mapOf("p" to path))
            it.addParameters(CommandLine.split(config.rawOpts))
        }
    }

    /**
     * Join commands into a single command.
     *
     * @param commands: one or more commands to execute
     * @return joined command
     */
    private fun joinCommands(vararg commands: PosixCommandLine): PosixCommandLine {
        val compoundCommand = commands.map { it.commandLineString }.joinToString(" && ")
        return PosixCommandLine("sh", "-c", compoundCommand)
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
    private var collectionsDir = ""
    private var rolesDir = ""
    private var force = false
    private var command = ""
    private var virtualEnv = ""
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
            }
            row {
                checkBox("Install transitive dependencies").bindSelected(::deps)
            }
            row("Collections directory:") {
                textFieldWithBrowseButton("Collections Directory").bindText(::collectionsDir)
            }
            row("Roles directory:") {
                textFieldWithBrowseButton("Roles Directory").bindText(::rolesDir)
            }
            row() {
                checkBox("Overwrite existing files").bindSelected(::force)
            }
            group("Environment") {
                row("Ansible command:") {
                    textFieldWithBrowseButton("Ansible Command").bindText(::command)
                }
                row("Python virtualenv:") {
                    textFieldWithBrowseButton("Python Virtual Environment",
                        fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                    ).bindText(::virtualEnv)
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
            collectionsDir = it.collectionsDir
            rolesDir = it.rolesDir
            command = it.command
            virtualEnv = it.virtualEnv
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
            it.collectionsDir = collectionsDir
            it.rolesDir = rolesDir
            it.command = command
            it.virtualEnv = virtualEnv
            it.rawOpts = rawOpts
            it.workDir = workDir
        }
    }
}
