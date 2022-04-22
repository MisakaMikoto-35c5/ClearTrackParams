package moe.wsl.lab.cleartrackparams.utils.data

import kotlinx.serialization.Serializable

@Serializable
data class RuleConfiguration(
    val webview_pre_execute: String
)
