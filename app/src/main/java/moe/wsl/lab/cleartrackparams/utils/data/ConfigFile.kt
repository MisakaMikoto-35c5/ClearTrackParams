package moe.wsl.lab.cleartrackparams.utils.data

import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.util.Log
import kotlinx.serialization.Serializable


@Serializable
data class ConfigFile(
    val rule_configuration: RuleConfiguration,
    val rules: Array<UrlItem>
) {
    companion object {
        fun readFromTomlText(fileContent: String): ConfigFile {
            val toml = com.moandjiezana.toml.Toml().read(fileContent)
            return toml.to(ConfigFile::class.java)
            //return com.akuleshov7.ktoml.Toml.decodeFromString<ConfigFile>(fileContent)
        }
    }
}
