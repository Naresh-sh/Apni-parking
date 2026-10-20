package com.surajpole.parkingfinder.data.local

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("apni_parking_prefs", Context.MODE_PRIVATE)

    var isPrivacyPolicyAccepted: Boolean
        get() = prefs.getBoolean(KEY_PRIVACY_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_PRIVACY_ACCEPTED, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var languageCode: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    fun getAppLanguage(): AppLanguage {
        val code = languageCode
        return AppLanguage.entries.find { it.code == code } ?: AppLanguage.ENGLISH
    }

    fun setAppLanguage(language: AppLanguage) {
        languageCode = language.code
    }

    companion object {
        private const val KEY_PRIVACY_ACCEPTED = "key_privacy_policy_accepted"
        private const val KEY_DARK_MODE = "key_dark_mode"
        private const val KEY_LANGUAGE = "key_language"
    }
}
