package software.mdklatt.idea.ansible.run

import com.intellij.execution.configurations.GeneralCommandLine
import org.apache.commons.text.StringTokenizer
import org.apache.commons.text.matcher.StringMatcherFactory


/**
 * Manage an external process that uses POSIX-style command line arguments.
 *
 * @param exePath: path to executable
 * @param arguments: positional arguments
 * @param options: POSIX-style options
 */
class PosixCommandLine(
    exePath: String,
    arguments: List<String> = emptyList(),
    options: Map<String, Any?> = emptyMap()
) : GeneralCommandLine() {

    companion object {
        /**
         * Split command line arguments using shell syntax.
         *
         * Arguments are split on whitespace. Quoted whitespace is preserved.
         * A repeated quote character is interpreted as a quote literal.
         *
         * @param args: argument expression to split
         * @return: sequence of arguments
         */
        fun split(args: String): List<String> {
            val delim = StringMatcherFactory.INSTANCE.splitMatcher()
            val quote = StringMatcherFactory.INSTANCE.quoteMatcher()
            val parser = StringTokenizer(args, delim, quote)
            return parser.tokenList
        }
    }

    init {
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
        // TODO: Is there a way to create an in-memory File object?
        val file = createTempFile()
        file.writeText(text)
        withInput(file)
        return this
    }
}
