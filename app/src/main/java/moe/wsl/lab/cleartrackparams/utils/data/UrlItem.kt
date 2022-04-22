package moe.wsl.lab.cleartrackparams.utils.data

import kotlinx.serialization.Serializable

@Serializable
data class UrlItem(
    val regex: String,
    val action: ClearAction,
    val luaScript: String? = null,
    val javascript: String? = null,
    val params: Array<String>? = null
) {
    @kotlinx.serialization.Transient
    val pattern: Regex get() = Regex(regex)
}
