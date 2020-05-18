package software.mdklatt.idea.ansible.run


import com.intellij.execution.Executor
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
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.CheckBox
import com.intellij.ui.layout.panel
import org.jdom.Element
import javax.swing.JComponent


/**
 * TODO
 */
class GalaxyConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance.
     */
    override fun createTemplateConfiguration(project: Project): GalaxyRunConfiguration {
        return GalaxyRunConfiguration(project, this, this.name)
    }

    /**
     * The name of the run configuration variant created by this factory.
     *
     * @returns: name
     */
    override fun getName(): String {
        return "Ansible Galaxy"
    }
}


/**
 * TODO
 */
class GalaxyRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<RunProfileState>(project, factory, name) {

    var settings = GalaxyRunSettings()

    /**
     * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
     * returned from this method may also implement [com.intellij.execution.impl.CheckableRunConfigurationEditor]. The returned object
     * can also implement [com.intellij.openapi.options.SettingsEditorGroup] if the settings it provides need to be displayed in
     * multiple tabs.
     *
     * @return the settings editor component.
     */
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return GalaxySettingsEditor(project)
    }

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        return GalaxyCommandLineState(this, environment)
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
class GalaxyCommandLineState(private val config: GalaxyRunConfiguration, environment: ExecutionEnvironment) : CommandLineState(environment) {
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
        val settings = config.settings
        val command = PosixCommandLine(settings.command, listOf("install"))
        val options = mutableMapOf<String, Any?>(
            "no-deps" to !settings.deps,
            "force" to settings.force,
            "role-file" to nullBlank(settings.requirements),
            "roles-path" to nullBlank(settings.rolesDir)
        )
        command.addOptions(options)
        command.addParameters(config.settings.options)
        if (!command.environment.contains("TERM")) {
            command.environment["TERM"] = "xterm-256color"
        }
        if (settings.workDir.isNotBlank()) {
            command.withWorkDirectory(settings.workDir)
        }
        val process = KillableColoredProcessHandler(command)
        ProcessTerminatedListener.attach(process, environment.project)
        return process
    }
}


class GalaxySettingsEditor(project: Project) : SettingsEditor<GalaxyRunConfiguration>() {

    var requirements = TextFieldWithBrowseButton()
    var deps = CheckBox("Install transitive dependencies")
    var force = CheckBox("Overwrite existing roles")
    var rolesDir = TextFieldWithBrowseButton()
    var options = RawCommandLineEditor()
    var command = TextFieldWithBrowseButton()
    var workDir = TextFieldWithBrowseButton()

    init {
        val fileChooser = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor()
        requirements.addBrowseFolderListener("Requirements", "", project, fileChooser)
        command.addBrowseFolderListener("Galaxy Command", "", project, fileChooser)
        val dirChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        rolesDir.addBrowseFolderListener("Roles Directory", "", project, dirChooser)
        workDir.addBrowseFolderListener("Working Directory", "", project, dirChooser)
    }

    private var settingsPanel = panel{
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        row("Requirements:") {
            requirements()
            deps()
        }
        row("Roles directory:") {
            rolesDir()
            force()
        }
        row("Raw options:") { options() }
        titledRow("Environment") {}
        row("Ansible command:") { command() }
        row("Working directory:") { workDir() }
    }

    /**
     * Reset editor fields from the saved configuration state.
     *
     * @param config: run configuration
     */
    override fun resetEditorFrom(config: GalaxyRunConfiguration) {
        requirements.text = config.settings.requirements
        deps.isSelected = config.settings.deps
        force.isSelected = config.settings.force
        rolesDir.text = config.settings.rolesDir
        command.text = config.settings.command
        options.text = PosixCommandLine.join(config.settings.options)
        workDir.text = config.settings.workDir
        return
    }

    override fun createEditor(): JComponent {
        return settingsPanel
    }

    override fun applyEditorTo(config: GalaxyRunConfiguration) {
        // This apparently gets called for every key press, so performance is
        // critical.
        config.settings.requirements = requirements.text
        config.settings.deps = deps.isSelected
        config.settings.force = force.isSelected
        config.settings.rolesDir = rolesDir.text
        config.settings.options = PosixCommandLine.split(options.text)
        config.settings.workDir = workDir.text
        return
    }
}


/**
 * Manage GalaxyRunConfiguration runtime settings.
 */
class GalaxyRunSettings {

    companion object {
        private const val DELIMIT = "|"
    }

    var requirements = ""
    var deps = true
    var force = false
    var rolesDir = ""
    var command = ""
        get() = if (field.isNotBlank()) field else "ansible-galaxy"
    var options = emptyList<String>()
        set(value) {
            field = if (value.size == 1 && value[0].isBlank()) emptyList() else value
        }
    var workDir = ""

    /**
     * Reading settings from a JDOM element.
     *
     * @param element: input element.
     */
    fun read(element: Element) {
        requirements = JDOMExternalizerUtil.readField(element, "requirements", "")
        force = JDOMExternalizerUtil.readField(element, "force", "false").toBoolean()
        deps = JDOMExternalizerUtil.readField(element, "deps", "deps").toBoolean()
        rolesDir = JDOMExternalizerUtil.readField(element, "rolesDir", "")
        command = JDOMExternalizerUtil.readField(element, "command", "")
        options = JDOMExternalizerUtil.readField(element, "options", "").split(DELIMIT)
        workDir = JDOMExternalizerUtil.readField(element, "workDir", "")
        return
    }

    /**
     * Write settings to a JDOM element.
     *
     * @param element: output element
     */
    fun write(element: Element) {
        // Value isn't written if it matches the given default.
        JDOMExternalizerUtil.writeField(element, "requirements", requirements, "")
        JDOMExternalizerUtil.writeField(element, "deps", deps.toString(), "")
        JDOMExternalizerUtil.writeField(element, "force", force.toString(), "")
        JDOMExternalizerUtil.writeField(element, "rolesDir", rolesDir, "")
        JDOMExternalizerUtil.writeField(element, "command", command, "")
        JDOMExternalizerUtil.writeField(element, "options", options.joinToString(DELIMIT), "")
        JDOMExternalizerUtil.writeField(element, "workDir", workDir, "")
        return
    }
}
