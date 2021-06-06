package software.mdklatt.idea.ansible.configurations

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.icons.AllIcons
import com.intellij.internal.statistic.beans.addBoolIfDiffers
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.util.getOrCreate
import java.util.*
import javax.swing.Icon
import org.jdom.Element


class AnsibleConfigurationType : ConfigurationTypeBase(
    "AnsibleConfigurationType",
    "Ansible",
    "Run Ansible commands",
    IconLoader.findIcon("/icons/ansibleMango.svg")  // relative to resources/
) {
    init {
        addFactory(GalaxyConfigurationFactory(this))
        addFactory(PlaybookConfigurationFactory(this))
    }
}


/**
 * Manage common Ansible configuration settings.
 */
internal abstract class AnsibleSettings protected constructor() {

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