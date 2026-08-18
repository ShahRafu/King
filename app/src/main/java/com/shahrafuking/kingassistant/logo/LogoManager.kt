package com.shahrafuking.kingassistant.logo

import android.content.Context

/**
 * LogoManager stores URIs (as strings) for external and internal logos in SharedPreferences.
 * External logo: what is shown outside the app (launcher / marketing) — stored as a URI string.
 * Internal logo: what is shown inside the app (top bar / splash) — stored as a URI string.
 */
class LogoManager(private val ctx: Context) {
    private val prefsName = "king_prefs"
    private val keyExternal = "logo_external_uri"
    private val keyInternal = "logo_internal_uri"

    private val prefs by lazy { ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }

    fun setExternalLogo(uriString: String?) {
        prefs.edit().putString(keyExternal, uriString).apply()
    }

    fun setInternalLogo(uriString: String?) {
        prefs.edit().putString(keyInternal, uriString).apply()
    }

    fun getExternalLogo(): String? = prefs.getString(keyExternal, null)
    fun getInternalLogo(): String? = prefs.getString(keyInternal, null)

    fun clearExternal() { setExternalLogo(null) }
    fun clearInternal() { setInternalLogo(null) }
}
