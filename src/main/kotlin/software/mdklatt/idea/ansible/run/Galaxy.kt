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
import com.intellij.util.getOrCreate
import org.jdom.Element
import javax.swing.JComponent


/**
 * Factory for GalaxyRunConfiguration instances.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#configuration-factory">Configuration Factory</a>
 */
class GalaxyConfigurationFactory internal constructor(type: ConfigurationType) : ConfigurationFactory(type) {
    /**
     * Creates a new template run configuration within the context of the specified project.
     *
     * @param project the project in which the run configuration will be used
     * @return the run configuration instance.
     */
    override fun createTemplateConfiguration(project: Project) =
            GalaxyRunConfiguration(project, this, "")

    /**
     * The name of the run configuration variant created by this factory.
     *
     * @returns: name
     */
    override fun getName() = "Ansible Galaxy"

    /**
     * Run configuration ID used for serialization.
     *
     * @return: unique ID
     */
    override fun getId() = this::class.java.simpleName
}


/**
 * Run Configuration for executing <a href="https://docs.ansible.com/ansible/latest/cli/ansible-galaxy.html">ansible-galaxy</a>.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#run-configuration">Run Configuration</a>
 */
class GalaxyRunConfiguration internal constructor(project: Project, factory: ConfigurationFactory, name: String) :
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
    override fun getConfigurationEditor() = GalaxySettingsEditor(project)

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
            GalaxyCommandLineState(this, environment)

    /**
     * Read settings from a JDOM element.
     *
     * This is part of the RunConfiguration persistence API.
     *
     * @param element: input element.
     */
    override fun readExternal(element: Element) {
        super.readExternal(element)
        settings = GalaxyRunSettings(element)
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
class GalaxyCommandLineState internal constructor(private val config: GalaxyRunConfiguration, environment: ExecutionEnvironment) :
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
        val settings = config.settings
        val command = PosixCommandLine(settings.command, listOf("install"))
        val options = mutableMapOf<String, Any?>(
            "no-deps" to !settings.deps,
            "force" to settings.force,
            "role-file" to nullBlank(settings.requirements),
            "roles-path" to nullBlank(settings.rolesDir)
        )
        command.addOptions(options)
        command.addParameters(PosixCommandLine.split(settings.rawOpts))
        if (!command.environment.contains("TERM")) {
            command.environment["TERM"] = "xterm-256color"
        }
        if (settings.workDir.isNotBlank()) {
            command.withWorkDirectory(settings.workDir)
        }
        return KillableColoredProcessHandler(command).also {
            ProcessTerminatedListener.attach(it, environment.project)
        }
    }
}


/**
 * UI component for Galaxy Run Configuration settings.
 *
 * @see <a href="https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#settings-editor">Settings Editor</a>
 */
class GalaxySettingsEditor internal constructor(project: Project) : SettingsEditor<GalaxyRunConfiguration>() {

    companion object {
        private val fileChooser = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor()
        private val dirChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor()
    }

    var requirements = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Requirements", "", project, fileChooser)
    }
    var deps = CheckBox("Install transitive dependencies")
    var force = CheckBox("Overwrite existing roles")
    var rolesDir = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Roles Directory", "", project, dirChooser)
    }
    var command = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Ansible Command", "", project, fileChooser)
    }
    var rawOpts = RawCommandLineEditor()
    var workDir = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Working Directory", "", project, dirChooser)
    }

    /**
     * Create the widget for this editor.
     *
     * @return UI widget
     */
    override fun createEditor(): JComponent {
        // https://www.jetbrains.org/intellij/sdk/docs/user_interface_components/kotlin_ui_dsl.html
        return panel {
            row("Requirements:") {
                requirements()
                deps()
            }
            row("Roles directory:") {
                rolesDir()
                force()
            }
            titledRow("Environment") {}
            row("Ansible command:") { command() }
            row("Raw options:") { rawOpts() }
            row("Working directory:") { workDir() }
        }
    }

    /**
     * Reset editor fields from the configuration state.
     *
     * @param config: run configuration
     */
    override fun resetEditorFrom(config: GalaxyRunConfiguration) {
        config.apply {
            requirements.text = settings.requirements
            deps.isSelected = settings.deps
            force.isSelected = settings.force
            rolesDir.text = settings.rolesDir
            rawOpts.text = settings.rawOpts
            workDir.text = settings.workDir

        }
        return
    }

    /**
     * Apply editor fields to the configuration state.
     *
     * @param config: run configuration
     */
    override fun applyEditorTo(config: GalaxyRunConfiguration) {
        // This apparently gets called for every key press, so performance is
        // critical.
        config.apply {
            settings = GalaxyRunSettings()
            settings.requirements = requirements.text
            settings.deps = deps.isSelected
            settings.force = force.isSelected
            settings.rolesDir = rolesDir.text
            settings.rawOpts = rawOpts.text
            settings.workDir = workDir.text
        }
        return
    }
}


/**
 * Manage GalaxyRunConfiguration runtime settings.
 */
class GalaxyRunSettings internal constructor() {

    companion object {
        private const val JDOM_TAG = "ansible-galaxy"
    }

    var requirements = ""
    var deps = true
    var force = false
    var rolesDir = ""
    var command = ""
        get() = if (field.isNotBlank()) field else "ansible-galaxy"
    var rawOpts = ""
    var workDir = ""

    /**
     * Construct object from a JDOM element.
     *
     * @param element: input element
     */
    internal constructor(element: Element) : this() {
        element.getOrCreate(JDOM_TAG).let {
            requirements = JDOMExternalizerUtil.readField(it, "requirements", "")
            deps = JDOMExternalizerUtil.readField(it, "deps", "true").toBoolean()
            force = JDOMExternalizerUtil.readField(it, "force", "false").toBoolean()
            rolesDir = JDOMExternalizerUtil.readField(it, "rolesDir", "")
            command = JDOMExternalizerUtil.readField(it, "command", "")
            rawOpts = JDOMExternalizerUtil.readField(it, "rawOpts", "")
            workDir = JDOMExternalizerUtil.readField(it, "workDir", "")
        }
        return
    }

    /**
     * Write settings to a JDOM element.
     *
     * @param element: output element
     */
    fun write(element: Element) {
        val child = element.getOrCreate(JDOM_TAG)
        JDOMExternalizerUtil.writeField(child, "requirements", requirements)
        JDOMExternalizerUtil.writeField(child, "deps", deps.toString())
        JDOMExternalizerUtil.writeField(child, "force", force.toString())
        JDOMExternalizerUtil.writeField(child, "rolesDir", rolesDir)
        JDOMExternalizerUtil.writeField(child, "command", command)
        JDOMExternalizerUtil.writeField(child, "rawOpts", rawOpts)
        JDOMExternalizerUtil.writeField(child, "workDir", workDir)
        return
    }
}
