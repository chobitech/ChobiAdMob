package com.chobitech.lib.admob

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

fun Context.createConsentRequestParameters(isDebugMode: Boolean, testDeviceHashedIds: List<String> = listOf()): ConsentRequestParameters {
    return ConsentRequestParameters
        .Builder()
        .also { builder ->
            when (isDebugMode) {
                true -> {
                    val debugSettings = ConsentDebugSettings.Builder(this)
                        .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                        .also { ds ->
                            testDeviceHashedIds.forEach {
                                ds.addTestDeviceHashedId(it)
                            }
                        }
                        .build()

                    builder.setConsentDebugSettings(debugSettings)
                }

                false -> {
                    builder.setTagForUnderAgeOfConsent(false)
                }
            }
        }
        .build()
}

open class ConsentFormLoadableComponentActivity : ComponentActivity() {

    protected lateinit var consentInformation: ConsentInformation
        private set

    protected lateinit var consentForm: ConsentForm
        private set

    private fun initializeAdMob() {
        val reqConfig = RequestConfiguration.Builder()
            .setTestDeviceIds(
                listOf(
                    AdRequest.DEVICE_ID_EMULATOR
                )
            )
            .build()

        MobileAds.setRequestConfiguration(reqConfig)

        MobileAds.initialize(this) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeAdMob()
        consentInformation = UserMessagingPlatform.getConsentInformation(this)
    }

    fun resetConsentState() {
        if (this::consentInformation.isInitialized) {
            consentInformation.reset()
        }
    }

    protected fun loadForm(onError: (FormError) -> Boolean = { false }, consentFormLoadResultReceiver: (Boolean) -> Unit) {
        UserMessagingPlatform.loadConsentForm(
            this,
            { cForm ->
                this.consentForm = cForm
                when (consentInformation.consentStatus) {
                    ConsentInformation.ConsentStatus.REQUIRED, ConsentInformation.ConsentStatus.UNKNOWN -> {
                        consentForm.show(this) { fError ->
                            fError?.also {
                                onError.invoke(it)
                            }
                            loadForm(onError, consentFormLoadResultReceiver)
                        }
                    }
                    else -> consentFormLoadResultReceiver(true)
                }
            },
            { fError ->
                consentFormLoadResultReceiver(onError(fError))
            }
        )
    }

    fun requestConsentInfoUpdate(
        params: ConsentRequestParameters,
        onRequestError: (FormError) -> Boolean = { false },
        onLoadFormError: (FormError) -> Boolean = { false },
        consentFormLoadResultReceiver: (Boolean) -> Unit
    ) {
        if (this::consentInformation.isInitialized) {
            consentInformation.requestConsentInfoUpdate(
                this,
                params,
                {
                    when (consentInformation.isConsentFormAvailable) {
                        true -> loadForm(onLoadFormError, consentFormLoadResultReceiver)
                        false -> {
                            //DebugMsg.i("consent Information form is not available, so turn to AD OK")
                            consentFormLoadResultReceiver(true)
                        }
                    }
                },
                { fError ->
                    consentFormLoadResultReceiver(onRequestError(fError))
                }
            )
        }
    }
}
