package ru.fpvladder.laps.trainer.usb.serialization

import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration

val UsbHidYamlFormat = Yaml(
    configuration = YamlConfiguration(
        singleLineStringStyle = SingleLineStringStyle.Plain,
    )
)
