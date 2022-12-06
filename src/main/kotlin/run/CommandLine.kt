/**
 * Extensions for the CommandLine class.
 */
package dev.mdklatt.idea.ansible.run

import dev.mdklatt.idea.common.exec.CommandLine
import kotlin.io.path.Path
import java.io.File
import kotlin.io.path.pathString


/**
 * Activate a Python virtualenv environment for execution.
 *
 * @param venvPath: path to virtualenv directory
 */
fun CommandLine.setPythonVenv(venvPath: String) {
    // Per virtualenv docs, all activators do is prepend the environment's bin/
    // directory to PATH. Per inspection of an installed 'activate' script, a
    // VIRTUAL_ENV variable is also set, and PYTHONHOME is unset if it exists.
    // <https://virtualenv.pypa.io/en/latest/user_guide.html#activators>
    val venv = Path(venvPath).toAbsolutePath()
    val path = venv.resolve("bin")
    val pathEnv = environment["PATH"] ?: System.getenv("PATH")
    withEnvironment(mapOf<String, Any?>(
        "VIRTUAL_ENV" to venv.pathString,
        "PATH" to listOf(path.pathString, pathEnv).joinToString(File.pathSeparator),
        "PYTHONHOME" to if (environment.containsKey("PYTHONHOME")) "" else null,
    ))
}


/**
 * Activate a Python virtualenv environment for execution.
 *
 * @param venvPath: path to virtualenv directory
 * @return modified instance
 */
fun CommandLine.withPythonVenv(venvPath: String): CommandLine {
    setPythonVenv(venvPath)
    return this
}