package software.mdklatt.idea.ansible.run

import com.intellij.execution.Executor
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.layout.panel
import org.jdom.Element
import javax.swing.JComponent
import javax.swing.JTextField


/**
 * TODO
 */
class PlaybookConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance.
     */
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return PlaybookRunConfiguration(project, this, "Ansible Playbook")
    }
}


/**
 * TODO
 */
class PlaybookRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
        LocatableConfigurationBase<RunProfileState>(project, factory, name) {

    var settings = PlaybookRunSettings()

    /**
     * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
     * returned from this method may also implement [com.intellij.execution.impl.CheckableRunConfigurationEditor]. The returned object
     * can also implement [com.intellij.openapi.options.SettingsEditorGroup] if the settings it provides need to be displayed in
     * multiple tabs.
     *
     * @return the settings editor component.
     */
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PlaybookSettingsEditor(project)
    }

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        return PlaybookCommandLineState(this.settings, environment)
    }

    /**
     * Read settings from a JDOM element.
     *
     * This is part of the RunConfiguration persistence API.
     *
     * @param element: input element.
     */
    override fun readExternal(element: Element) {
        super.readExternal(element)
        settings.read(element)
        return
    }

    /**
     * Write settings to a JDOM element.
     *
     * This is part of the RunConfiguration persistence API.

     * @param element: output element.
     */
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        settings.write(element)
        return
    }
}


/**
 * TODO
 */
class PlaybookSettingsEditor(project: Project) : SettingsEditor<PlaybookRunConfiguration>() {

    var playbooks = TextFieldWithBrowseButton()
    var inventory = TextFieldWithBrowseButton()
    var host = JTextField("")
    var tags = JTextField("")
    var variables = JTextField("")
    var options = RawCommandLineEditor()
    var command = TextFieldWithBrowseButton()
    var workdir = TextFieldWithBrowseButton()

    init {
        val fileChooser = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor()
        playbooks.addBrowseFolderListener("Playbooks", "", project, fileChooser)
        inventory.addBrowseFolderListener("Inventory", "", project, fileChooser)
        command.addBrowseFolderListener("Playbook Command", "", project, fileChooser)
        val dirChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        workdir.addBrowseFolderListener("Working Directory", "", project, dirChooser)
    }

    private var settingsPanel = panel{
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        row("Playbooks:") { playbooks() }
        row("Inventory:") { inventory() }
        row("Host:") { host() }
        row("Tags:") { tags() }
        row("Extra variables:") { variables() }
        row("Raw options:") { options() }
        titledRow("Ansible Settings") {}
        row("Playbook command:") { command() }
        row("Working directory:") { workdir() }
    }

    /**
     * Reset editor fields from the saved configuration state.
     *
     * @param config: run configuration
     */
    override fun resetEditorFrom(config: PlaybookRunConfiguration) {
        playbooks.text = if (config.settings.playbooks.isNotEmpty()) config.settings.playbooks[0] else ""
        inventory.text = if (config.settings.inventory.isNotEmpty()) config.settings.inventory[0] else ""
        host.text = config.settings.host
        tags.text = config.settings.tags.joinToString(" ")
        variables.text = config.settings.variables.joinToString(" ")
        command.text = config.settings.command
        options.text = PosixCommandLine.join(config.settings.options)
        workdir.text = config.settings.workdir
        return
    }

    override fun createEditor(): JComponent {
        return settingsPanel
    }

    override fun applyEditorTo(config: PlaybookRunConfiguration) {
        // This apparently gets called for every key press, so performance is
        // critical.
        // TODO: Get file list for playbooks and inventory.
        config.settings.playbooks = listOf(playbooks.text)
        config.settings.inventory = listOf(inventory.text)
        config.settings.host = host.text
        config.settings.tags = tags.text.split(" ")
        config.settings.variables = variables.text.split(" ")
        config.settings.options = PosixCommandLine.split(options.text)
        config.settings.workdir = workdir.text
        return
    }
}



/**
 * TODO
 */
class PlaybookCommandLineState(private val settings: PlaybookRunSettings, environment: ExecutionEnvironment) :
        CommandLineState(environment) {

    /**
     * Starts the process.
     *
     * @return the handler for the running process
     * @throws ExecutionException if the execution failed.
     * @see GeneralCommandLine
     *
     * @see com.intellij.execution.process.OSProcessHandler
     */
    override fun startProcess(): ProcessHandler {
        fun nullBlank(str: String): String? {
            return if (str.isNotBlank()) str else null
        }
        val command = PosixCommandLine(settings.command)
        val options = mutableMapOf<String, Any?>(
            "limit" to nullBlank(settings.host),
            "inventory" to nullBlank(settings.inventory.joinToString(",")),
            "tags" to nullBlank(settings.tags.joinToString(",")),
            "extra-vars" to nullBlank(settings.variables.joinToString(" "))
        )
        command.addOptions(options)
        command.addParameters(settings.options)
        command.addParameters(settings.playbooks)
        if (!command.environment.contains("TERM")) {
            command.environment["TERM"] = "xterm-256color"
        }
        if (settings.workdir.isNotBlank()) {
            command.withWorkDirectory(settings.workdir)
        }
        val process = KillableColoredProcessHandler(command)
        ProcessTerminatedListener.attach(process, environment.project)
        return process
    }
}


/**
 * Manage PlaybookRunConfiguration runtime settings.
 */
class PlaybookRunSettings {

    companion object {
        private const val DELIMIT = "|"
    }

    var playbooks = emptyList<String>()  // TODO: add set() for [""] -> []
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }
    var inventory = emptyList<String>()
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }
    var host = ""
    var tags = emptyList<String>()  // TODO: Set
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }
    var variables = emptyList<String>()  // TODO: Set
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }
    var command = ""
        get() = if (field.isNotBlank()) field else "ansible-playbook"
    var options = emptyList<String>()
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }
    var workdir = ""

    /**
     * Reading settings from a JDOM element.
     *
     * @param element: input element.
     */
    fun read(element: Element) {
        playbooks = JDOMExternalizerUtil.readField(element, "playbooks", "").split(DELIMIT)
        inventory = JDOMExternalizerUtil.readField(element, "inventory", "").split(DELIMIT)
        host = JDOMExternalizerUtil.readField(element, "host", "")
        tags = JDOMExternalizerUtil.readField(element, "tags", "").split(DELIMIT)
        variables = JDOMExternalizerUtil.readField(element, "variables", "").split(DELIMIT)
        command = JDOMExternalizerUtil.readField(element, "command", "")
        options = JDOMExternalizerUtil.readField(element, "options", "").split(DELIMIT)
        workdir = JDOMExternalizerUtil.readField(element, "workDir", "")
        return
    }

    /**
     * Write settings to a JDOM element.
     *
     * @param element: output element
     */
    fun write(element: Element) {
        // Value isn't written if it matches the given default.
        JDOMExternalizerUtil.writeField(element, "playbooks", playbooks.joinToString(DELIMIT), "")
        JDOMExternalizerUtil.writeField(element, "inventory", inventory.joinToString(DELIMIT), "")
        JDOMExternalizerUtil.writeField(element, "host", host, "")
        JDOMExternalizerUtil.writeField(element, "tags", tags.joinToString(DELIMIT), "")
        JDOMExternalizerUtil.writeField(element, "variables", variables.joinToString(DELIMIT), "")
        JDOMExternalizerUtil.writeField(element, "command", command, "")
        JDOMExternalizerUtil.writeField(element, "options", options.joinToString(DELIMIT), "")
        JDOMExternalizerUtil.writeField(element, "workDir", workdir, "")
        return
    }
}
