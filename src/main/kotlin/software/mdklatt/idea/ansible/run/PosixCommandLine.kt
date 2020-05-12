package software.mdklatt.idea.ansible.run

import com.intellij.execution.configurations.GeneralCommandLine
import org.apache.commons.text.StringTokenizer
import org.apache.commons.text.matcher.StringMatcherFactory
import java.lang.StringBuilder


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
        private val quoteMatch = StringMatcherFactory.INSTANCE.quoteMatcher()
        private val splitMatch = StringMatcherFactory.INSTANCE.splitMatcher()

        /**
         * Join command line arguments using shell syntax
         *
         * Arguments containing whitespace are quoted, and quote literals are
         * escaped.
         */
        fun join(argv: List<String>): String {
            val quote = '"'
            val _argv = argv.toMutableList()
            val it = _argv.listIterator()
            while (it.hasNext()) {
                val str = StringBuilder()
                for (char in it.next()) {
                    str.append(char)
                    if (char == quote) {
                        // Use repeated character to escape quote literal.
                        str.append(char)
                    }
                }
                var arg = str.toString()
                if (splitMatch.isMatch(arg.toCharArray(), 0, 0, arg.lastIndex) > 0) {
                    arg = quote + arg + quote
                }
                it.set(arg)
            }
            return _argv.joinToString(" ")
        }

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
            val splitter = StringTokenizer(args, splitMatch, quoteMatch)
            return splitter.tokenList
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
