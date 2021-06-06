package software.mdklatt.idea.ansible.configurations

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.util.getOrCreate
import java.util.*
import javax.swing.Icon
import org.jdom.Element


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
     * The ID of the configuration type. Should be camel-cased without dashes, underscores, spaces and quotation marks.
     * The ID is used to store run configuration settings in a project or workspace file and
     * must not change between plugin versions.
     */
    override fun getId(): String = this::class.java.simpleName


    /**
     * Returns the display name of the configuration type. This is used, for example, to represent the configuration type in the run
     * configurations tree, and also as the name of the action used to create the configuration.
     *
     * @return the display name of the configuration type.
     */
    override fun getDisplayName() = "Ansible"

    /**
     * Returns the description of the configuration type. You may return the same text as the display name of the configuration type.
     *
     * @return the description of the configuration type.
     */
    override fun getConfigurationTypeDescription() = "Run Ansible commands"

    /**
     * Returns the configuration factories used by this configuration type. Normally each configuration type provides just a single factory.
     * You can return multiple factories if your configurations can be created in multiple variants (for example, local and remote for an
     * application server).
     *
     * @return the run configuration factories.
     */
    override fun getConfigurationFactories() = arrayOf(
            GalaxyConfigurationFactory(this),
            PlaybookConfigurationFactory(this)
        )
}


/**
 * Manage common Ansible configuration settings.
 */
abstract class AnsibleSettings protected constructor() {


    protected abstract val commandName: String
    internal abstract val xmlTagName: String

    protected val logger = Logger.getInstance(this::class.java)  // runtime class resolution
    protected var id: UUID? = null

    var command = ""
        get() = field.ifEmpty { commandName }
        set(value) { field = value.ifEmpty { commandName }}
    var rawOpts = ""
    var workDir = ""

    /**
     * Load stored settings.
     *
     * @param element:
     */
    internal open fun load(element: Element) {
        element.getOrCreate(xmlTagName).let {
            val str = JDOMExternalizerUtil.readField(it, "id", "")
            id = if (str.isEmpty()) UUID.randomUUID() else UUID.fromString(str)
            logger.debug("loading settings for configuration ${id.toString()}")
            command = JDOMExternalizerUtil.readField(it, "command", "")
            rawOpts = JDOMExternalizerUtil.readField(it, "rawOpts", "")
            workDir = JDOMExternalizerUtil.readField(it, "workDir", "")
        }
        return
    }

    /**
     * Save settings.
     *
     * @param element: JDOM element
     */
    internal open fun save(element: Element) {
        val default = element.getAttributeValue("default")?.toBoolean() ?: false
        element.getOrCreate(xmlTagName).let {
            if (!default) {
                id = id ?: UUID.randomUUID()
                logger.debug("saving settings for configuration ${id.toString()}")
                JDOMExternalizerUtil.writeField(it, "id", id.toString())
            }
            else {
                logger.debug("saving settings for default configuration")
            }
            JDOMExternalizerUtil.writeField(it, "command", command)
            JDOMExternalizerUtil.writeField(it, "rawOpts", rawOpts)
            JDOMExternalizerUtil.writeField(it, "workDir", workDir)
        }
        return
    }
}