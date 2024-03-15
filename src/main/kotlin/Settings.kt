/**
 * Ansible tool settings.
 */
package dev.mdklatt.idea.ansible

import com.intellij.openapi.components.*
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.UiDslUnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.*
import dev.mdklatt.idea.common.map.findFirstKey
import kotlin.io.path.Path


// Adapted from <https://github.com/tomblachut/svelte-intellij>


/**
 * Python execution target types.
 */
enum class InstallType { SYSTEM, VIRTUALENV, DOCKER }


/**
 * Get project-level Ansible settings.
 */
fun getAnsibleSettings(project: Project): AnsibleSettingsComponent = project.service<AnsibleSettingsComponent>()


/**
 * Serializable Ansible settings.
 */
class AnsibleSettingsState : BaseState() {
    var configFile by string()
    var installType by enum(InstallType.SYSTEM)
    var ansibleLocation by string("ansible")
}


/**
 * Persistent project-level Ansible settings.
 */
@Service(Service.Level.PROJECT)
@State(name = "AnsibleSettingsComponent", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class AnsibleSettingsComponent: SimplePersistentStateComponent<AnsibleSettingsState>(AnsibleSettingsState()) {
    /**
     * Resolve a command path for the configured Ansible installation.
     *
     * @param command: command name
     * @return executable command path
     */
    fun resolveAnsiblePath(command: String): String {
        return when (state.installType) {
            InstallType.SYSTEM, InstallType.DOCKER -> {
                Path(state.ansibleLocation ?: "ansible").parent?.resolve(command)?.toString() ?: command
            }
            InstallType.VIRTUALENV -> {
                // This assumes that the virtualenv is in $PATH.
                // See CommandLine.withPythonVenv()
                command
            }
        }
    }
}


/**
 * Manage project-level Ansible settings.
 */
class AnsibleSettingsConfigurable(project: Project): UiDslUnnamedConfigurable.Simple(), Configurable {

    private val settings = getAnsibleSettings(project)
    private val installTypeOptions = mapOf(
        InstallType.SYSTEM to "Ansible executable:",
        InstallType.VIRTUALENV to "Python virtualenv:",
        InstallType.DOCKER to "Docker image:",
    )

    // Need local versions of settings to bind to UI widgets  for some unknown
    // reason. Based on examples like the Svelte plugin, it should be possible
    // to bind directly to settings.state attribute.
    // TODO: Can settings.state attributes be used as delegates here?

    private var installType: InstallType
        get() = settings.state.installType
        set(value) {
            settings.state.installType = value
        }

    private var ansibleLocation: String
        get() = settings.state.ansibleLocation ?: ""
        set(value) {
            settings.state.ansibleLocation = value
        }

    private var configFile: String
        get() = settings.state.configFile ?: ""
        set(value) {
            settings.state.configFile = value
        }

    /**
     * Create the UI component for defining settings.
     */
    override fun Panel.createContent() {
        row {
            comboBox(installTypeOptions.values).let {
                it.bindItem(
                    getter = { installTypeOptions[installType] },
                    setter = { installType = installTypeOptions.findFirstKey(it)!! }
                )
                it.onChanged {
                    // Various attempts to implement this have failed, such as
                    // reassigning the value of locationField (see below),
                    // calling createContent() on the parent component, and
                    // removing locationField before createLocationField() is
                    // called.

                    // Update the location input field to reflect the new
                    // install type. Cannot use `installType` property here
                    // because changes have not been applied yet.
                    // TODO: Would that it were this simple, but it is not.
                    //val newInstallType = installTypeOptions.findFirstKey(it.selectedItem)!!
                    //locationField = createLocationField(newInstallType)
                }
            }

            // This should be a dynamic field based on the install type, but
            // attempts to update this dynamically when installType changes
            // have not been successful (see above). For now, use the least
            // common denominator, a plain text field.
            // TODO: createLocationField(this, installType)
            textField().bindText(::ansibleLocation)
        }
        row("Config file:") {
            textFieldWithBrowseButton(
                "Ansible Config File",
                fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(),
            ).bindText(::configFile)
        }
    }

    /**
     * Get the display name.
     */
    // TODO: Where exactly is this used, because the IDE uses the name in plugin.xml.
    override fun getDisplayName() = "Ansible"

    /**
     * Create the input component for the Ansible location field.
     *
     * @param row: row containing the field
     * @param installType: installation type
     */
    private fun createLocationField(row: Row, installType: InstallType): Cell<*> {
        return when (installType) {
            InstallType.SYSTEM -> row.textFieldWithBrowseButton(
                "Ansible Executable",
                fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(),
            ).bindText(::ansibleLocation)

            InstallType.VIRTUALENV -> row.textFieldWithBrowseButton(
                "Python Virtualenv",
                fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            ).bindText(::ansibleLocation)

            InstallType.DOCKER -> row.textField().bindText(::ansibleLocation)
        }
    }
}
