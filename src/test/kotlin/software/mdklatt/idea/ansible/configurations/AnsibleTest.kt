/**
 * Unit tests for the Ansible module.
 */
package software.mdklatt.idea.ansible.configurations

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull


/**
 * Unit tests for the AnsibleConfigurationType class.
 */
internal class AnsibleConfigurationTypeTest {

    private var type = AnsibleConfigurationType()

    /**
     * Test the id property.
     */
    @Test
    fun testId() {
        assertTrue(type.id.isNotBlank())
    }

    /**
     * Test the icon property.
     */
    @Test
    fun testIcon() {
        assertNotNull(type.icon)
    }

    /**
     * Test the configurationTypeDescription property.
     */
    @Test
    fun testConfigurationTypeDescription() {
        assertTrue(type.configurationTypeDescription.isNotEmpty())
    }

    /**
     * Test the displayName property.
     */
    @Test
    fun testDisplayName() {
        assertTrue(type.displayName.isNotEmpty())
    }

    /**
     * Test the configurationFactories property.
     */
    @Test
    fun testConfigurationFactories() {
        assertTrue(type.configurationFactories.isNotEmpty())
    }
}
