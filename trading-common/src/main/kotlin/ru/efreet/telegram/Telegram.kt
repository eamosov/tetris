package ru.efreet.telegram

import com.google.gson.GsonBuilder
import org.apache.commons.io.IOUtils
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.client.ProxyAuthenticationStrategy
import java.util.*


class Telegram(val tocken: String, val proxyHost: String, val proxyPort: Int, val proxyUser: String, val proxyPass: String) {

    val gson = GsonBuilder().setPrettyPrinting().create()

    val credsProvider = BasicCredentialsProvider()

    var chatId: String? = null

    init {
        credsProvider.setCredentials(AuthScope(proxyHost, proxyPort), UsernamePasswordCredentials(proxyUser, proxyPass))
    }

    var client = HttpClients.custom()
            .setProxy(HttpHost(proxyHost, proxyPort))
            .setDefaultCredentialsProvider(credsProvider)
            .setProxyAuthenticationStrategy(ProxyAuthenticationStrategy())
            .build()


    fun post(method: String, params: Map<String, String>): String {

        val target = HttpHost("api.telegram.org", 443, "https")
        val request = HttpPost("/bot${tocken}/${method}")
        request.entity = StringEntity(gson.toJson(params), ContentType.APPLICATION_JSON)

        client.execute(target, request).use { response ->
            return IOUtils.toString(response.entity.content)
        }
    }

    fun sendMessage(chatId: String, text: String): String {
        return post("sendMessage", mapOf(Pair("chat_id", chatId), Pair("text", text)))
    }

    fun sendMessage(text: String): String {
        return sendMessage(chatId!!, text)
    }

    companion object {
        fun create(): Telegram {
            val prop = Properties()
            prop.load(this.javaClass.classLoader.getResourceAsStream("application.properties"))
            return Telegram(prop.getProperty("TELEGRAM_TOCKEN"),
                    prop.getProperty("TELEGRAM_PROXY_HOST"),
                    prop.getProperty("TELEGRAM_PROXY_PORT").toInt(),
                    prop.getProperty("TELEGRAM_PROXY_USER"),
                    prop.getProperty("TELEGRAM_PROXY_PASS")).also {
                it.chatId = prop.getProperty("TELEGRAM_CHAT_ID")
            }
        }
    }
}