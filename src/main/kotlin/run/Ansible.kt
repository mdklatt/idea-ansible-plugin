package dev.mdklatt.idea.ansible.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.IconLoader
import dev.mdklatt.idea.common.exec.PosixCommandLine


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
    internal abstract fun getCommand(): PosixCommandLine
}
