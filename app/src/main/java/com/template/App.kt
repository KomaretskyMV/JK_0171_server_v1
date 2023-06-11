package com.template

import android.app.Application
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging

class App : Application() {

    lateinit var firestoreDb: FirebaseFirestore
    var task: Task<DocumentSnapshot>? = null

    override fun onCreate() {
        super.onCreate()
        Firebase.analytics
        Firebase.messaging
        firestoreDb = Firebase.firestore
//        task = Firebase.firestore
//            .collection("database")
//            .document("check")
//            .get()
    }
}