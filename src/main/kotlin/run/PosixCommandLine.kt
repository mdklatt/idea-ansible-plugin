/**
 * Extensions for the PosixCommandLine class.
 */
package dev.mdklatt.idea.ansible.run

import dev.mdklatt.idea.common.exec.PosixCommandLine
import kotlin.io.path.Path
import java.io.File
import kotlin.io.path.pathString


/**
 * Activate a Python virtualenv for execution.
 *
 * @param venvPath: path to virtualenv directory
 * @return modified instance
 */
internal fun PosixCommandLine.withPythonVenv(venvPath: String): PosixCommandLine {
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
internal fun PosixCommandLine.withConfigFile(configPath: String): PosixCommandLine {
    // <https://docs.ansible.com/ansible/latest/reference_appendices/config.html#the-configuration-file>
    withEnvironment(mapOf<String, Any?>(
        "ANSIBLE_CONFIG" to configPath,
    ))
    return this
}


/**
 * Convert command to a `docker run` command.
 *
 * The command's currently defined environment will be passed into the Docker
 * container. Environment variables that should be applied to local `docker`
 * execution need to be defined on the return value of this method.
 *
 * on the result of this
 * @param image: Docker image name
 * @param workDir: local working directory
 * @param dockerExe: local Docker executable
 * @param venvPath: remote Python virtualenv directory
 */
internal fun PosixCommandLine.asDockerRun(image: String, venvPath: String? = null, dockerExe: String?): PosixCommandLine {
    val dockerEnv = environment.toMutableMap()
    if (venvPath != null) {
        // This needs to be done independently of withPythonVenv(), which
        // defines its variable using the local environment.
        val venv = Path(venvPath)
        val path = venv.resolve("bin")
        dockerEnv["VIRTUAL_ENV"] = venv.toString()
        dockerEnv["PATH"] = "${path}:\$PATH"
        dockerEnv["PYTHONHOME"] = "\$PYTHONHOME"
    }
    val localWorkDir = Path(workDirectory?.path ?: "").toAbsolutePath()
    val remoteWorkDir = "/tmp/ansible"  // TODO: configurable
    val dockerOpts = mapOf<String, Any>(
        "rm" to true,
        "env" to dockerEnv.entries.map { (name, value) -> "${name}='${value}'" },
        "workdir" to remoteWorkDir,
        "volume" to "${localWorkDir}:${remoteWorkDir}",
        "entrypoint" to exePath
    )
    val dockerCommand = PosixCommandLine(dockerExe ?: "docker", "run").also {
        it.addOptions(dockerOpts)
        it.addParameters(image)
        it.addParameters(*parametersList.list.toTypedArray())
    }
    return dockerCommand
}
