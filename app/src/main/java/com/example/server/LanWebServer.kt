package com.example.server

import android.content.Context
import com.example.LocalNetApp
import com.example.vpn.LocalNetVpnService
import com.example.vpn.VpnStatus
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.InputStream

class LanWebServer(
    private val context: Context,
    val port: Int = 8080
) {
    val pairingManager = PairingManager()
    private var server: EmbeddedServer<*, *>? = null

    fun start() {
        if (server != null) return

        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        buildJsonObject {
                            put("error", cause.message ?: "Internal Server Error")
                        }
                    )
                }
            }

            routing {
                // Static Web Dashboard Assets
                get("/") {
                    val indexHtml = loadAssetFile("dashboard/index.html")
                    if (indexHtml != null) {
                        call.respondText(indexHtml, ContentType.Text.Html)
                    } else {
                        call.respondText("LocalNetGuard LAN Dashboard Ready. Pair via QR code.", ContentType.Text.Plain)
                    }
                }

                // REST API V1
                route("/api/v1") {
                    get("/status") {
                        val vpnState = LocalNetVpnService.vpnStatus.value.name
                        val totalConn = runBlocking { LocalNetApp.instance.repository.totalConnectionsCount.first() }
                        val blockedConn = runBlocking { LocalNetApp.instance.repository.blockedConnectionsCount.first() }

                        call.respondText(
                            buildJsonObject {
                                put("app", "LocalNetGuard")
                                put("vpnStatus", vpnState)
                                put("totalConnections", totalConn)
                                put("blockedConnections", blockedConn)
                                put("serverPort", port)
                            }.toString(),
                            ContentType.Application.Json
                        )
                    }

                    get("/pairing-code") {
                        val code = pairingManager.getCurrentCode()
                        call.respondText(
                            buildJsonObject {
                                put("pairingCode", code)
                            }.toString(),
                            ContentType.Application.Json
                        )
                    }

                    post("/auth/pair") {
                        val code = call.request.queryParameters["code"] ?: ""
                        val sessionToken = pairingManager.validateCode(code)
                        if (sessionToken != null) {
                            call.respondText(
                                buildJsonObject {
                                    put("success", true)
                                    put("sessionToken", sessionToken)
                                }.toString(),
                                ContentType.Application.Json
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.Unauthorized,
                                buildJsonObject {
                                    put("error", "Invalid or expired pairing code")
                                }.toString()
                            )
                        }
                    }

                    get("/connections") {
                        val logs = runBlocking { LocalNetApp.instance.repository.recentConnections.first() }
                        call.respondText(
                            Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(com.example.data.model.ConnectionLog.serializer()), logs),
                            ContentType.Application.Json
                        )
                    }

                    get("/rules") {
                        val rules = runBlocking { LocalNetApp.instance.repository.customRules.first() }
                        call.respondText(
                            Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(com.example.data.model.FirewallRule.serializer()), rules),
                            ContentType.Application.Json
                        )
                    }

                    get("/dns/logs") {
                        val logs = runBlocking { LocalNetApp.instance.repository.recentDnsLogs.first() }
                        call.respondText(
                            Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(com.example.data.model.DnsLog.serializer()), logs),
                            ContentType.Application.Json
                        )
                    }
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    private fun loadAssetFile(path: String): String? {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }
}
