package ru.fpvladder.laps.trainer.model.serialization

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy

val YamlFormat = Yaml(
    configuration = YamlConfiguration(
        yamlNamingStrategy = YamlNamingStrategy.SnakeCase,
        polymorphismStyle = PolymorphismStyle.Tag,
        singleLineStringStyle = SingleLineStringStyle.PlainExceptAmbiguous,
        encodeDefaults = true,
    )
)
