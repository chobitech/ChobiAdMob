package com.chobitech.lib.admob

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal const val chobiAdMobBannerDataStoreName = "chobi_ad_banner_data_store"
internal val Context.chobiAdMobBannerDataStore: DataStore<Preferences> by preferencesDataStore(chobiAdMobBannerDataStoreName)


@Composable
fun rememberDefaultBannerSizeProvider(): AdSize {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    return remember(config.orientation) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, AdSize.FULL_WIDTH)
    }
}


@Composable
fun ChobiAdMobBanner(
    modifier: Modifier = Modifier,
    clickIntervalTimeMs: Long = 10 * 60 * 1000L,
    bannerUnitId: String,
    arrowAdShowFlagProvider: () -> Boolean = { true },
    isTest: Boolean,
    bannerSize: AdSize = rememberDefaultBannerSizeProvider(),
    adRequestProvider: () -> AdRequest = remember {
        {
            AdRequest.Builder().build()
        }
                                                  },
    bannerModifier: Modifier = Modifier,
    adListener: AdListener? = null
) {
    val context = LocalContext.current
    val cScope = rememberCoroutineScope()

    val arrowAdShow by remember(arrowAdShowFlagProvider) {
        derivedStateOf {
            arrowAdShowFlagProvider()
        }
    }

    var adLoadFailed by remember {
        mutableStateOf(false)
    }

    val dataStore = remember {
        context.chobiAdMobBannerDataStore
    }
    val lastClickedTimeKey = remember {
        longPreferencesKey("chobi_ad_banner_last_clicked_time")
    }
    val lastClickedTimeFlow = remember(dataStore) {
        dataStore.data.map {
            it[lastClickedTimeKey] ?: 0L
        }
    }

    val unitId = remember(bannerUnitId, isTest) {
        ChobiAdMob.getBannerId(bannerUnitId, isTest)
    }

    var isBannerShow by remember {
        mutableStateOf(false)
    }

    val bannerAdListener = remember(adListener) {
        object : AdListener() {
            override fun onAdClicked() {
                cScope.launch {
                    dataStore.edit {
                        it[lastClickedTimeKey] = System.currentTimeMillis()
                        isBannerShow = false
                        adListener?.onAdClicked()
                    }
                }
            }

            override fun onAdClosed() {
                adListener?.onAdClosed()
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                isBannerShow = false
                adLoadFailed = true
                adListener?.onAdFailedToLoad(p0)
            }

            override fun onAdImpression() {
                adListener?.onAdImpression()
            }

            override fun onAdLoaded() {
                adListener?.onAdLoaded()
            }

            override fun onAdOpened() {
                adListener?.onAdOpened()
            }

            override fun onAdSwipeGestureClicked() {
                adListener?.onAdSwipeGestureClicked()
            }
        }
    }


    Box(
        modifier = modifier
    ) {
        if (arrowAdShow && !adLoadFailed && isBannerShow) {
            AndroidView(
                modifier = bannerModifier,
                factory = { c ->
                    AdView(c).also { adv ->
                        adv.setAdSize(bannerSize)
                        adv.adUnitId = unitId
                        adv.adListener = bannerAdListener
                        adv.loadAd(adRequestProvider())
                    }
                }
            )
        }
    }

    LaunchedEffect(isBannerShow) {
        if (!isBannerShow) {
            isBannerShow = when (clickIntervalTimeMs <= 0L) {
                true -> true
                false -> {
                    val waitMs = clickIntervalTimeMs - (System.currentTimeMillis() - lastClickedTimeFlow.first())
                    when (waitMs <= 0L) {
                        true -> true
                        false -> {
                            delay(waitMs)
                            true
                        }
                    }
                }
            }
        }
    }
}
