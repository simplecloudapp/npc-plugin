package app.simplecloud.npc.core.repository

import app.simplecloud.plugin.api.shared.config.ConfigMigrator
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File

class YamlConfigurator<E : Any>(
    private val clazz: Class<E>,
    private val migrator: ConfigMigrator? = null,
    private val prepare: (ConfigurationNode) -> Unit = {},
) {

    fun load(file: File): E? {
        val loader = loader(file)
        val node = loader.load()
        prepare(node)
        if (migrator?.migrate(node) == true) loader.save(node)

        return node.get(clazz)
    }

    fun save(file: File, entity: E) {
        val loader = loader(file)
        val node = loader.createNode()
        node.set(clazz, entity)
        loader.save(node)
    }

    private fun loader(file: File): YamlConfigurationLoader = YamlConfigurationLoader.builder()
        .nodeStyle(NodeStyle.BLOCK)
        .defaultOptions { options ->
            options.implicitInitialization(false).serializers { serializers ->
                serializers
                    .register(LenientEnumSerializer::handles, LenientEnumSerializer)
                    .registerAnnotatedObjects(ObjectMapper.factory())
            }
        }
        .file(file)
        .build()
}
