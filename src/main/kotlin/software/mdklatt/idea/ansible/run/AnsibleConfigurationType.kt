package software.mdklatt.idea.ansible.run


import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon


class AnsibleConfigurationType : ConfigurationType {
    /**
     * Returns the 16x16 icon used to represent the configuration type.
     *
     * @return: the icon
     */
    override fun getIcon(): Icon {
        // https://www.jetbrains.org/intellij/sdk/docs/reference_guide/work_with_icons_and_images.html
        val url = this.javaClass.classLoader.getResource("icons/ansibleMango.svg")
        return IconLoader.findIcon(url, true) ?: AllIcons.General.GearPlain
    }

    /**
     * Returns the description of the configuration type. You may return the same text as the display name of the configuration type.
     *
     * @return the description of the configuration type.
     */
    override fun getConfigurationTypeDescription(): String {
        return "Run Ansible commands"
    }

    /**
     * The ID of the configuration type. Should be camel-cased without dashes, underscores, spaces and quotation marks.
     * The ID is used to store run configuration settings in a project or workspace file and
     * must not change between plugin versions.
     */
    override fun getId(): String {
        return "AnsibleConfigurationType"
    }

    /**
     * Returns the display name of the configuration type. This is used, for example, to represent the configuration type in the run
     * configurations tree, and also as the name of the action used to create the configuration.
     *
     * @return the display name of the configuration type.
     */
    override fun getDisplayName(): String {
        return "Ansible"
    }

    /**
     * Returns the configuration factories used by this configuration type. Normally each configuration type provides just a single factory.
     * You can return multiple factories if your configurations can be created in multiple variants (for example, local and remote for an
     * application server).
     *
     * @return the run configuration factories.
     */
    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(
            // Registering multiple ConfigurationFactories here is supposed to
            // permit Configuration subtypes (cf. PyCharm Python test). This
            // doesn't work correctly for some reason. There are multiple
            // Configurations show in the menu, but they are all duplicates of
            // the first factory in this list. Also, this ConfigurationType's
            // display name is show for each option instead of the display
            // name of the Configuration.
            // https://www.jetbrains.org/intellij/sdk/docs/basics/run_configurations/run_configuration_management.html#configuration-factory
            // GalaxyConfigurationFactory(this),  // FIXME
            PlaybookConfigurationFactory(this)
        )
    }
}
