/**
 * Galaxy run configuration for executing `ansible-galaxy`.
 *
 * <https://docs.ansible.com/ansible/latest/galaxy/user_guide.html>
 */
package dev.mdklatt.idea.ansible.run

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.*
import dev.mdklatt.idea.common.exec.CommandLine
import dev.mdklatt.idea.common.exec.PosixCommandLine
import kotlinx.serialization.Serializable
import java.io.FileNotFoundException
import kotlin.io.path.Path


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
class GalaxyOptions : AnsibleOptions() {
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
    override fun getConfigurationEditor() = GalaxyEditor()

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
            val commands = mutableListOf<PosixCommandLine>()
            parseRequirements().let {
                if (it.collections != null) {
                    commands.add(createCommand("collection", collectionsDir))
                }
                if (it.roles != null) {
                    commands.add(createCommand("role", rolesDir))
                }
            }
            PosixCommandLine.andCommands(commands.asSequence()).also {
                it.withEnvironment(mapOf<String, Any?>(
                    // Silence warnings about unconfigured directories, which
                    // only matter when the collections and/or roles are
                    // consumed by `ansible-playbook`.
                    "ANSIBLE_COLLECTIONS_PATHS" to collectionsDir,
                    "ANSIBLE_ROLES_PATH" to rolesDir,
                ))
            }
        }
        return command.also {
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
        return PosixCommandLine(config.ansibleCommand, subcommand).also {
            it.addOptions(commonOptions + mapOf("p" to path))
            it.addParameters(CommandLine.splitArguments(config.rawOpts))
        }
    }

    /**
     * Parse the requirements file.
     *
     * @return requirements data
     */
    private fun parseRequirements(): Requirements {
        val file = Path(config.workDir).resolve(config.requirements).toFile()
        return if (file.exists()) {
            Yaml.default.decodeFromStream(file.inputStream())
        } else {
            throw FileNotFoundException("Unknown requirements file '${config.requirements}'")
        }
    }
}


/**
 * UI component for Galaxy Run Configuration settings.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#settings-editor">Settings Editor</a>
 */
class GalaxyEditor internal constructor() : AnsibleEditor<GalaxyOptions, GalaxyRunConfiguration>() {

    private var requirements = ""
    private var deps = false
    private var collectionsDir = ""
    private var rolesDir = ""
    private var force = false

    /**
     * Add command-specific settings to the UI component.
     *
     * @param parent: parent component builder
     */
    override fun addCommandFields(parent: Panel) {
        parent.let {
            it.row("Requirements:") {
                textFieldWithBrowseButton("Requirements File").bindText(::requirements)
            }
            it.row {
                checkBox("Install transitive dependencies").bindSelected(::deps)
            }
            it.row("Collections directory:") {
                textFieldWithBrowseButton("Collections Directory").bindText(::collectionsDir)
            }
            it.row("Roles directory:") {
                textFieldWithBrowseButton("Roles Directory").bindText(::rolesDir)
            }
            it.row() {
                checkBox("Overwrite existing files").bindSelected(::force)
            }
        }
    }

    /**
     * Reset UI with command options from configuration.
     *
     * @param config: run configuration
     */
    override fun resetCommandOptions(config: GalaxyRunConfiguration) {
        // Update bound properties from config value then reset UI.
        config.let {
            requirements = it.requirements
            deps = it.deps
            force = it.force
            collectionsDir = it.collectionsDir
            rolesDir = it.rolesDir
        }
    }

    /**
     * Apply command options from UI to configuration.
     *
     * @param config: run configuration
     */
    override fun applyCommandOptions(config: GalaxyRunConfiguration) {
        config.let {
            it.requirements = requirements
            it.deps = deps
            it.force = force
            it.collectionsDir = collectionsDir
            it.rolesDir = rolesDir
        }
    }
}


/**
 * Schema for an Ansible requirements file.
 *
 * @see <a href="https://docs.ansible.com/ansible/latest/galaxy/user_guide.html#install-multiple-collections-with-a-requirements-file">requirements files</a>
 */
@Serializable
data class Requirements(
    val roles: List<Map<String, String>>? = null,
    val collections: List<Map<String, String>>? = null,
)
