package moe.wsl.lab.cleartrackparams.utils

import android.content.Context
import android.util.Log
import androidx.room.Room
import moe.wsl.lab.cleartrackparams.utils.data.ConfigFile
import moe.wsl.lab.cleartrackparams.utils.data.RuleConfiguration
import moe.wsl.lab.cleartrackparams.utils.data.UrlItem
import moe.wsl.lab.cleartrackparams.utils.localdb.Subscription
import moe.wsl.lab.cleartrackparams.utils.localdb.SubscriptionDb
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.NullPointerException
import java.lang.StringBuilder
import java.net.URI


class SubscriptionManager(private val context: Context) {
    var client = OkHttpClient()
    private var subscriptionDb: SubscriptionDb? = null

    fun getSubscriptionDb(): SubscriptionDb {
        if (subscriptionDb == null) {
            subscriptionDb = Room.databaseBuilder(
                context.applicationContext,
                SubscriptionDb::class.java, "SubscriptionDb"
            )
                .fallbackToDestructiveMigration()
                .build()
            checkHasAnyRules()
        }
        return subscriptionDb as SubscriptionDb
    }

    fun close() {
        getSubscriptionDb().close()
        subscriptionDb = null
    }

    fun commit() {
        close()
        getSubscriptionDb()
    }

    fun addSubscription(name: String, url: String) {
        val subscriptionDao = getSubscriptionDb().subscriptionDao()
        val subscriptionContent = getSubscriptionContent(url)
        val configFile = ConfigFile.readFromTomlText(subscriptionContent)
        if (configFile.rules == null) {
            throw NullPointerException()
        }
        if (configFile.rule_configuration == null) {
            throw NullPointerException()
        }
        if (configFile.rule_configuration.webview_pre_execute == null) {
            throw NullPointerException()
        }
        if (configFile.rule_configuration.common_track_args == null) {
            throw NullPointerException()
        }
        val subscription = Subscription(
            id = 0,
            subscriptionName = name,
            subscriptionURL = url,
            lastUpdateAt = System.currentTimeMillis() / 1000L,
            content = subscriptionContent
        )
        subscriptionDao.insertAll(subscription)
    }

    fun getSubscriptionContent(url: String): String {
        val uri = URI(url)
        return when (uri.scheme) {
            "http", "https" -> {
                getSubscriptionViaInternet(url)
            }
            "internal" -> {
                Log.d("test", uri.rawPath)
                getSubscriptionViaAssets(uri.rawPath)
            }
            else -> {
                throw NotImplementedError("Not supported scheme: ${uri.scheme}")
            }
        }
    }

    private fun getSubscriptionViaInternet(url: String): String {
        val request: Request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()
        return response.body!!.string()
    }

    private fun getSubscriptionViaAssets(filePath: String): String {
        val fileContent = StringBuilder()
        val fileStream = context.assets.open(filePath.substring(1))
        val bufferedReader = BufferedReader(InputStreamReader(fileStream))
        var line: String? = bufferedReader.readLine()
        while (line != null) {
            fileContent.append(line + '\n')
            line = bufferedReader.readLine()
        }
        return fileContent.toString()
    }

    fun getMixedRules(): ConfigFile {
        val subscriptionDao = getSubscriptionDb().subscriptionDao()
        val rules = subscriptionDao.getAll()
        val webViewPreExecute = StringBuilder()
        val commonTrackArgs = arrayListOf<String>()
        val urlItems = arrayListOf<UrlItem>()
        for (i in rules) {
            if (i.content.isNullOrEmpty()) {
                continue
            }
            val configFile = ConfigFile.readFromTomlText(i.content)
            webViewPreExecute
                .append('\n')
                .append(configFile.rule_configuration.webview_pre_execute)
            if (configFile.rule_configuration.common_track_args != null) {
                commonTrackArgs.addAll(configFile.rule_configuration.common_track_args)
            }
            urlItems.addAll(configFile.rules)
        }
        return ConfigFile(
            RuleConfiguration(
                webViewPreExecute.toString(),
                commonTrackArgs.toTypedArray()
            ),
            urlItems.toTypedArray()
        )
    }

    private fun checkHasAnyRules() {
        if (getSubscriptionDb().subscriptionDao().getCount() == 0) {
            addSubscription("Embed Rules", "internal:///embed_rules.toml")
        }
    }
}