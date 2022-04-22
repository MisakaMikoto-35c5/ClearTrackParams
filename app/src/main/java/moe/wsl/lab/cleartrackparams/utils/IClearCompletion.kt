package moe.wsl.lab.cleartrackparams.utils

import java.lang.Exception

interface IClearCompletion {
    fun completion(content: String)
    fun failed(e: Throwable)
}