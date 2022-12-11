/**
 * Base implementation of run configurations for executing Ansible commands.
 */
package dev.mdklatt.idea.ansible.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.dsl.builder.*
import org.jdom.Element
import java.lang.RuntimeException
import java.util.*


/**
 * Run configuration type for Ansible execution.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configuration-management.html?from=jetbrains.org#configuration-type">Configuration Type</a>
 */
class AnsibleConfigurationType : ConfigurationTypeBase(
    "AnsibleConfigurationType",
    "Ansible",
    "Run Ansible commands",
    IconLoader.getIcon("/icons/ansibleMango.svg", AnsibleConfigurationType::class.java)
) {
    init {
        addFactory(GalaxyConfigurationFactory(this))
        addFactory(PlaybookConfigurationFactory(this))
    }
}


/**
 * Handle persistence of run configuration options.
 *
 * This base class defines options common to all Ansible configurations.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-configurationfactory">Run Configurations Tutorial</a>
 */
abstract class AnsibleOptions(ansibleCommand: String) : RunConfigurationOptions() {
    internal var uid by string()
    internal var command by string(ansibleCommand)
    internal var virtualEnv by string()
    internal var rawOpts by string()
    internal var workDir by string()
}


/**
 * Base class for Ansible run configurations.
 *
 * This base class defines options common to all run configurations.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
abstract class AnsibleRunConfiguration<Options : AnsibleOptions>(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
    private val ansibleCommand: String,
) :
    RunConfigurationBase<Options>(project, factory, name) {

    protected val logger = Logger.getInstance(this::class.java)

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
    internal var command: String
        get() = options.command ?: ""
        set(value) {
            options.command = value.ifBlank { ansibleCommand }
        }
    internal var virtualEnv: String
        get() = options.virtualEnv ?: ""
        set(value) {
            options.virtualEnv = value
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
    final override fun getOptions(): Options {
        // Kotlin considers this an unsafe cast because generics do not have
        // runtime type information unless they are reified, which is not
        // supported for class parameters.
        @Suppress("UNCHECKED_CAST")
        return super.getOptions() as Options
    }

    /**
     * Read settings from XML.
     */
    override fun readExternal(element: Element) {
        super.readExternal(element)
        if (options.uid == null) {
            options.uid = UUID.randomUUID().toString()
        }
    }

    /**
     * Write settings to XML.
     */
    override fun writeExternal(element: Element) {
        val default = element.getAttributeValue("default")?.toBoolean() ?: false
        if (default) {
            // Do not save UID with configuration template.
            options.uid = null
        }
        super.writeExternal(element)
    }
}


/**
 * Base class for run configuration command line processes.
 *
 * @param environment: execution environment
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#implement-a-run-configuration">Run Configurations Tutorial</a>
 */
abstract class AnsibleCommandLineState internal constructor(environment: ExecutionEnvironment) :
    CommandLineState(environment) {

    protected val logger = Logger.getInstance(this::class.java)

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
        val command = getCommand().also {
            if (!it.environment.contains("TERM")) {
                it.environment["TERM"] = "xterm-256color"
            }
        }
        return KillableColoredProcessHandler(command).also {
            ProcessTerminatedListener.attach(it, environment.project)
        }
    }

    /**
     * Get command to execute.
     *
     * @return command
     */
    internal abstract fun getCommand(): GeneralCommandLine
}


/**
 * Base class for run configuration UI
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/run-configurations.html#bind-the-ui-form">Run Configurations Tutorial</a>
 */
abstract class AnsibleEditor<Options : AnsibleOptions, Config : AnsibleRunConfiguration<Options>> protected constructor() :
    SettingsEditor<Config>() {

    private var command = ""
    private var virtualEnv = ""
    private var rawOpts = ""
    private var workDir = ""

    /**
     * Create the UI component.
     *
     * @return Swing component
     */
    final override fun createEditor(): DialogPanel {
        return panel {
            group("Command Settings") {
                addCommandFields(this)
            }
            group("Ansible Settings") {
                addAnsibleFields(this)
            }
        }
    }

    /**
     * Add common Ansible settings to the UI component.
     *
     * @param parent: parent component builder
     */
    private fun addAnsibleFields(parent: Panel) {
        parent.let {
            it.row("Ansible command:") {
                textFieldWithBrowseButton("Ansible Command").bindText(::command)
            }
            it.row("Python virtualenv:") {
                textFieldWithBrowseButton("Python Virtual Environment",
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                ).bindText(::virtualEnv)
            }
            it.row("Raw options:") {
                expandableTextField().bindText(::rawOpts)
            }
            it.row("Working directory:") {
                textFieldWithBrowseButton("Working Directory",
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                ).bindText(::workDir)
            }
        }
    }

    /**
     * Reset UI with options from configuration.
     *
     * @param config: run configuration
     */
    final override fun resetEditorFrom(config: Config) {
        // Update bound properties from config values then reset UI.
        resetAnsibleOptions(config)
        resetCommandOptions(config)
        (this.component as DialogPanel).reset()
    }

    /**
     * Reset UI with Ansible options from configuration.
     *
     * @param config: run configuration
     */
    private fun resetAnsibleOptions(config: Config) {
        config.let {
            command = it.command
            virtualEnv = it.virtualEnv
            rawOpts = it.rawOpts
            workDir = it.workDir
        }
    }

    /**
     * Apply UI options to configuration.
     *
     * @param config: run configuration
     */
    final override fun applyEditorTo(config: Config) {
        // Apply UI to bound properties then update config values.
        (this.component as DialogPanel).apply()
        applyAnsibleOptions(config)
        applyCommandOptions(config)
    }

    /**
     * Apply Ansible options from UI to configuration.
     *
     * @param config: run configuration
     */
    private fun applyAnsibleOptions(config: Config) {
        config.let {
            it.command = command
            it.virtualEnv = virtualEnv
            it.rawOpts = rawOpts
            it.workDir = workDir
        }
    }

    /**
     * Add command-specific settings to the UI component.
     *
     * @param parent: parent component builder
     */
    protected abstract fun addCommandFields(parent: Panel)

    /**
     * Reset UI with command options from configuration.
     *
     * @param config: run configuration
     */
    protected abstract fun resetCommandOptions(config: Config)

    /**
     * Apply command options from UI to configuration.
     *
     * @param config: run configuration
     */
    protected abstract fun applyCommandOptions(config: Config)
}
