package software.mdklatt.idea.ansible.run


import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.CommonProgramParametersPanel
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import javax.swing.JComponent


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


class PlaybookRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<RunProfileState>(project, factory, name) {
    /**
     * Returns the UI control for editing the run configuration settings. If additional control over validation is required, the object
     * returned from this method may also implement [com.intellij.execution.impl.CheckableRunConfigurationEditor]. The returned object
     * can also implement [com.intellij.openapi.options.SettingsEditorGroup] if the settings it provides need to be displayed in
     * multiple tabs.
     *
     * @return the settings editor component.
     */
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PlaybookSettingsEditor()
    }

    /**
     * Prepares for executing a specific instance of the run configuration.
     *
     * @param executor the execution mode selected by the user (run, debug, profile etc.)
     * @param environment the environment object containing additional settings for executing the configuration.
     * @return the RunProfileState describing the process which is about to be started, or null if it's impossible to start the process.
     */
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        TODO("Not yet implemented")
    }
}


class PlaybookSettingsEditor : SettingsEditor<PlaybookRunConfiguration>() {
    override fun resetEditorFrom(s: PlaybookRunConfiguration) {
        // TODO
        return
    }

    override fun createEditor(): JComponent {
        return PlaybookSettingsPanel()
    }

    override fun applyEditorTo(s: PlaybookRunConfiguration) {
        // TODO
        return
    }
}


private class PlaybookSettingsPanel : CommonProgramParametersPanel() {

}
