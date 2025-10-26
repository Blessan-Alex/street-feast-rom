package com.streatfeast.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class StreetFeastApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // MOCK MODE: Firebase disabled for prototype
        /*
        FirebaseApp.initializeApp(this)
        
        // Enable offline persistence for Firestore
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestore.firestoreSettings = settings
        */
    }
}


