/**
 * Extensions for the CommandLine class.
 */
package dev.mdklatt.idea.ansible.run

import dev.mdklatt.idea.common.exec.CommandLine
import kotlin.io.path.Path
import java.io.File
import kotlin.io.path.pathString


/**
 * Activate a Python virtualenv for execution.
 *
 * @param venvPath: path to virtualenv directory
 * @return modified instance
 */
internal fun CommandLine.withPythonVenv(venvPath: String): CommandLine {
    // Per virtualenv docs, all activators do is prepend the environment's bin/
    // directory to PATH. Per inspection of an installed 'activate' script, a
    // VIRTUAL_ENV variable is also set, and PYTHONHOME is unset if it exists.
    // <https://virtualenv.pypa.io/en/latest/user_guide.html#activators>
    val venv = Path(venvPath).toAbsolutePath()
    val path = venv.resolve("bin")
    val pathEnv = environment["PATH"] ?: System.getenv("PATH")
    val homeEnv = environment["PYTHONHOME"] ?: System.getenv("PYTHONHOME")
    withEnvironment(mapOf<String, Any?>(
        "VIRTUAL_ENV" to venv.pathString,
        "PATH" to listOf(path.pathString, pathEnv).joinToString(File.pathSeparator),
        "PYTHONHOME" to if (homeEnv != null) "" else null,  // null to ignore
    ))
    return this
}


/**
 * Specify an Ansible config file.
 *
 * @param configPath: config file path
 * @return modified instance
 */
internal fun CommandLine.withConfigFile(configPath: String): CommandLine {
    // <https://docs.ansible.com/ansible/latest/reference_appendices/config.html#the-configuration-file>
    withEnvironment(mapOf<String, Any?>(
        "ANSIBLE_CONFIG" to configPath,
    ))
    return this
}


/**
 * Convert command to a `docker run` command.
 */
internal fun CommandLine.asDockerRun(image: String, dockerExe: String? = null): CommandLine {
    // TODO: "dockerExe run --rm -v workingDir:/tmp/ansible -w /tmp/ansible  [-e <get from this.environment>] dockerImage ..."
    TODO("Docker execution not implemented")
}
