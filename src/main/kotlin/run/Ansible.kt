package dev.mdklatt.idea.ansible.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import org.jdom.Element
import java.lang.RuntimeException
import java.util.*


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
