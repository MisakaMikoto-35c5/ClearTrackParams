package moe.wsl.lab.cleartrackparams.utils.data

import kotlinx.serialization.Serializable

@Serializable
data class MatchedUrlItem(
    val url: String,
    val urlItem: UrlItem,
)