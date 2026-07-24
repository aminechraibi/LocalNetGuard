package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "localnet_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val IS_VPN_ENABLED = booleanPreferencesKey("is_vpn_enabled")
        val DEFAULT_RULE_ACTION = stringPreferencesKey("default_rule_action")
        val IS_LAN_SERVER_ENABLED = booleanPreferencesKey("is_lan_server_enabled")
        val LAN_SERVER_PORT = intPreferencesKey("lan_server_port")
        val LAN_ADMIN_PASSWORD_HASH = stringPreferencesKey("lan_admin_password_hash")
        val IS_HTTPS_INSPECTION_ENABLED = booleanPreferencesKey("is_https_inspection_enabled")
        val IS_QUIC_BLOCKED_GLOBALLY = booleanPreferencesKey("is_quic_blocked_globally")
        val UPSTREAM_DNS_TYPE = stringPreferencesKey("upstream_dns_type")
        val UPSTREAM_DNS_CUSTOM = stringPreferencesKey("upstream_dns_custom")
        val DOH_URL = stringPreferencesKey("doh_url")
        val DEVELOPER_PAYLOAD_CAPTURE = booleanPreferencesKey("developer_payload_capture")
        val LOG_RETENTION_DAYS = intPreferencesKey("log_retention_days")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: "SYSTEM"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    val isVpnEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_VPN_ENABLED] ?: false
    }

    suspend fun setVpnEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_VPN_ENABLED] = enabled
        }
    }

    val defaultRuleAction: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_RULE_ACTION] ?: "ALLOW"
    }

    suspend fun setDefaultRuleAction(action: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_RULE_ACTION] = action
        }
    }

    val isLanServerEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_LAN_SERVER_ENABLED] ?: false
    }

    suspend fun setLanServerEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LAN_SERVER_ENABLED] = enabled
        }
    }

    val lanServerPort: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAN_SERVER_PORT] ?: 8080
    }

    suspend fun setLanServerPort(port: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAN_SERVER_PORT] = port
        }
    }

    val isHttpsInspectionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_HTTPS_INSPECTION_ENABLED] ?: false
    }

    suspend fun setHttpsInspectionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_HTTPS_INSPECTION_ENABLED] = enabled
        }
    }

    val isQuicBlockedGlobally: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_QUIC_BLOCKED_GLOBALLY] ?: false
    }

    suspend fun setQuicBlockedGlobally(blocked: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_QUIC_BLOCKED_GLOBALLY] = blocked
        }
    }

    val upstreamDnsType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.UPSTREAM_DNS_TYPE] ?: "SYSTEM"
    }

    val dohUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DOH_URL] ?: "https://cloudflare-dns.com/dns-query"
    }

    suspend fun setUpstreamDnsConfig(type: String, customIp: String = "1.1.1.1", dohUrl: String = "https://cloudflare-dns.com/dns-query") {
        context.dataStore.edit { prefs ->
            prefs[Keys.UPSTREAM_DNS_TYPE] = type
            prefs[Keys.UPSTREAM_DNS_CUSTOM] = customIp
            prefs[Keys.DOH_URL] = dohUrl
        }
    }
}
