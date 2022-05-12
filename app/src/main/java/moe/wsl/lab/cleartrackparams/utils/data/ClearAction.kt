package moe.wsl.lab.cleartrackparams.utils.data

import kotlinx.serialization.Serializable

@Serializable
enum class ClearAction(val action: String) {
    WEB_VIEW("WEB_VIEW"),
    LUA("LUA"),
    PARAM_BLACKLIST("PARAM_BLACKLIST"),
    PARAM_WHITELIST("PARAM_WHITELIST"),
    CLEAR_ALL_PARAM("CLEAR_ALL_PARAM"),
    SHORT_LINK("SHORT_LINK")
}