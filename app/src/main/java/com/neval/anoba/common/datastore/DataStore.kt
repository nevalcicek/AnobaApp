package com.neval.anoba.common.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// DataStore için bir eklenti (extension property) oluşturuluyor.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "anoba_settings")
