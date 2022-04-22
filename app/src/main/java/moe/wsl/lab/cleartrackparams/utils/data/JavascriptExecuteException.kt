package moe.wsl.lab.cleartrackparams.utils.data

import java.lang.Exception

class JavascriptExecuteException(message: String) : Exception() {

    var javascriptErrorMessage: String? = null

    override fun toString(): String {
        return super.toString() + "\n$javascriptErrorMessage"
    }
}