package com.surajpole.parkingfinder.data.local

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("apni_parking_prefs", Context.MODE_PRIVATE)

    var isPrivacyPolicyAccepted: Boolean
        get() = prefs.getBoolean(KEY_PRIVACY_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_PRIVACY_ACCEPTED, value).apply()

    companion object {
        private const val KEY_PRIVACY_ACCEPTED = "key_privacy_policy_accepted"
    }
}
