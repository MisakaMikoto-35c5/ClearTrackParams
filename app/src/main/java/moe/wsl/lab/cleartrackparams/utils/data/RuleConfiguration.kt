package moe.wsl.lab.cleartrackparams.utils.data

import android.util.Log
import kotlinx.serialization.Serializable

@Serializable
data class RuleConfiguration(
    val webview_pre_execute: String,
    val common_track_args: Array<String>?
) {

    @kotlinx.serialization.Transient
    val common_track_args_pattern: Array<Regex> get() {
        if (common_track_args == null) {
            Log.w("Configuration", "common_track_args is null!")
            return emptyArray()
        }
        val regexList = arrayListOf<Regex>()
        for (i in common_track_args) {
            regexList.add(Regex(i))
        }
        return regexList.toTypedArray()
    }
}
