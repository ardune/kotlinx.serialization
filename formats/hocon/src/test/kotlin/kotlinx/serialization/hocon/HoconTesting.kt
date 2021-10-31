package kotlinx.serialization.hocon

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kotlinx.serialization.KSerializer
import org.junit.Assert.assertEquals

internal inline fun <reified T : Any> Hocon.assertStringFormAndRestored(
    expected: String,
    original: T,
    serializer: KSerializer<T>,
    printResult: Boolean = false,
) {
    val expectedConfig = ConfigFactory.parseString(expected)
    val config = this.encodeToConfig(serializer, original)
    if (printResult) println("[Serialized form] $config")
    assertEquals(expectedConfig, config)
    val restored = this.decodeFromConfig(serializer, config)
    if (printResult) println("[Restored form] $restored")
    assertEquals(original, restored)
}

internal fun assertConfigEquals(expected: String, actual: Config) {
    assertEquals(ConfigFactory.parseString(expected), actual)
}
