package com.chobitech.lib.admob

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

object ChobiAdMob {

    const val userPrefsName = "com.chobitech.lib.android.admob_user_prefs"

    const val testBannerId = "ca-app-pub-3940256099942544/6300978111"
    const val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
    const val testInterstitialMovieId = "ca-app-pub-3940256099942544/8691691433"

    private fun getIdString(id: String, testId: String, isTest: Boolean): String = when (isTest) {
        true -> testId
        false -> id
    }

    fun getBannerId(id: String, isTest: Boolean) = getIdString(id, testBannerId, isTest)
    fun getInterstitialId(id: String, isTest: Boolean) = getIdString(id, testInterstitialId, isTest)
    fun getInterstitialMovieId(id: String, isTest: Boolean) = getIdString(id, testInterstitialMovieId, isTest)


    var isInitialized: Boolean = false
        private set

    fun initialize(context: Context) {
        if (isInitialized) {
            return
        }

        val reqConfig = RequestConfiguration.Builder()
            .setTestDeviceIds(listOf(
                AdRequest.DEVICE_ID_EMULATOR
            ))
            .build()

        MobileAds.setRequestConfiguration(reqConfig)

        MobileAds.initialize(context) {

        }

        isInitialized = true
    }
}
