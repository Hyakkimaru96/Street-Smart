package com.example.streetsmart

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val _locationText = MutableLiveData<String>()
    val locationText: LiveData<String> get() = _locationText

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission") // Assume permissions are checked in the Activity
    fun getLocation(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val address = addressList?.firstOrNull()?.getAddressLine(0)
                    _locationText.value = address ?: "Unknown address"
                } catch (e: Exception) {
                    _locationText.value = "Failed to fetch address"
                }
            } else {
                _locationText.value = "Unable to fetch location"
            }
        }.addOnFailureListener {
            _locationText.value = "Failed to fetch location"
        }
    }
}
