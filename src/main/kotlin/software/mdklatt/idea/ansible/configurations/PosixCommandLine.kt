package software.mdklatt.idea.ansible.configurations

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.util.execution.ParametersListUtil
import java.nio.CharBuffer


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


    private var inputData: ByteArray? = null

    /**
     * Construct an instance.
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
     * Send input to the external command.
     *
     * To protect sensitive data, the input buffer is cleared after the command
     * is executed, so this must be called prior to each invocation.
     *
     * @param input: STDIN contents
     * @return self reference
     *
     * @see #withInput(File?)
     */
    fun withInput(input: String): PosixCommandLine {
        inputData = input.toByteArray()
        return this
    }

    /**
     * Send input to the external command.
     *
     * To protect sensitive data, the input buffer is cleared after the command
     * is executed, so this must be called prior to each invocation.
     *
     * @param input: STDIN contents
     * @return: self reference
     *
     * @see #withInput(File?)
     */
    fun withInput(input: CharArray): PosixCommandLine {
        val bytes = Charsets.UTF_8.encode(CharBuffer.wrap(input))
        inputData = bytes.array()
        return this
    }

    /**
     * Final configuration of the ProcessBuilder before starting the process.
     *
     * @param builder: filled ProcessBuilder
     * @return
     */
    protected override fun buildProcess(builder: ProcessBuilder): ProcessBuilder {
        // Override the base class to redirect STDIN so it can be written to
        // once the process has been started, cf. createProcess().
        if (inputData != null) {
            builder.redirectInput(ProcessBuilder.Redirect.PIPE)
        }
        return builder
    }

    /**
     * Create and start the external process.
     *
     * @return external process
     */
    override fun createProcess(): Process {
        // Override the base class to write to STDIN.
        val process = super.createProcess()
        if (inputData != null) {
            process.apply {
                // The Process instance's output stream is actually STDIN from
                // the external command's point of view.
                outputStream.write(inputData!!)
                outputStream.close()
            }
            inputData!!.fill(0)  // clear any sensitive data from memory
            inputData = null
        }
        return process
    }
}
