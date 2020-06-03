package software.mdklatt.idea.ansible.run

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.util.execution.ParametersListUtil


/**
 * Manage an external process that uses POSIX-style command line arguments.
 */
class PosixCommandLine() : GeneralCommandLine() {

    companion object {
        /**
         * Join command line arguments using shell syntax.
         *
         * Arguments containing whitespace are quoted, and quote literals are
         * escaped with a backslash. This matches the behavior of the
         * {@link import com.intellij.ui.RawCommandLineEditor} class.
         *
         */
        fun join(argv: List<String>) = ParametersListUtil.join(argv)

        /**
         * Split command line arguments using shell syntax.
         *
         * Arguments are split on whitespace. Quoted whitespace is preserved.
         * A literal quote character must be escaped with a backslash. This
         * matches the behavior of the
         * {@link import com.intellij.ui.RawCommandLineEditor} class.
         *
         * Note that this does *not* work for `--flag=value` style options;
         * these should be specified as `--flag value` instead, where `value`
         * is quoted if it contains spaces.
         *
         * @param args: argument expression to split
         * @return: sequence of arguments
         */
        fun split(args: String): List<String> = ParametersListUtil.parse(args)
    }

    /**
     *
     */
    internal constructor(exePath: String, arguments: List<String> = emptyList(),
            options: Map<String, Any?> = emptyMap()): this() {
        withExePath(exePath)
        addOptions(options)
        addParameters(arguments)
    }

    /**
     * Append POSIX-style options to the command line.
     *
     * Boolean values are treated as a switch and are emitted with no value
     * (true) or ignored (false). Null-valued options are ignored.
     *
     * @param options: mapping of option flags and values
     */
    fun addOptions(options: Map<String, Any?> = emptyMap()): PosixCommandLine {
        for ((key, value) in options) {
            if (value == null || (value is Boolean && !value)) {
                // Switch is off, ignore option.
                continue
            }
            addParameter("--$key")
            if (value !is Boolean) {
                // Not a switch, use value.
                addParameter(value.toString())
            }
        }
        return this
    }

    /**
     * Redirect input.
     *
     * @param text: text value to use as input
     * @return self reference
     *
     * @see #withInput(File?)
     */
    fun withInput(text: String): PosixCommandLine {
        // This is problematic for working with sensitive data, e.g. password
        // prompts. While the temporary file will be deleted upon exit from the
        // JVM, there are no doubt failure modes that prevent the file from
        // being deleted. Even if deleting is successful, presumably the JVM
        // doesn't exit until the IDE is closed, so the sensitive data could
        // persist on disk for a long time.
        createTempFile().let {
            it.deleteOnExit()
            it.writeText(text)
            withInput(it)
        }
        return this
    }

//    /**
//     * Create the external process used to execute this command.
//     *
//     * @return external process
//     */
//    override fun createProcess(): Process {
//        // This is an attempt redirecting STDIN to a subprocess via a memory
//        // buffer instead of a disk file (see withInput). Unfortunately,
//        // neither the base class API nor the ProcessBuilder API (see
//        // buildProcess) provide a way to access the underlying Process until
//        // after it has been started, at which point it's too late to modify
//        // its I/O handles.
//        // FIXME: Doesn't work because process has already been started.
//        return super.createProcess().apply {
//            // Note that the Process's _output_ stream is used to push data to
//            // the subprocess _input_ stream,
//            // TODO: Use text from withInput() call.
//            outputStream.write("Data for STDIN".toByteArray())
//            outputStream.flush()
//        }
//    }
}
