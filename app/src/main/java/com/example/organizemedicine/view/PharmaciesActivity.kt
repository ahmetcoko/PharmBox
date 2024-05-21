package com.example.organizemedicine.view

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.organizemedicine.R
import com.example.organizemedicine.databinding.ActivityPharmaciesBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONObject


class PharmaciesActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityPharmaciesBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private var trackBoolean: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPharmaciesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        registerLauncher()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        sharedPreferences = this.getSharedPreferences("com.example.organizemedicine.view", MODE_PRIVATE)
        trackBoolean = false

        binding.btnFetchPharmacies.setOnClickListener {
            fetchDutyPharmacies("İzmir")
        }
        setupBottomNavigation()

    }
    private fun setupBottomNavigation() {
        binding.bottomMenu.setItemSelected(R.id.bottom_Pharmacies)
        binding.bottomMenu.setOnItemSelectedListener {
            when (it) {
                R.id.bottom_upload -> startActivity(Intent(this, PostUploadActivity::class.java))
                R.id.bottom_search -> startActivity(Intent(this, SearchActivity::class.java))
                R.id.bottom_home -> startActivity(Intent(this, MedicineFeedActivity::class.java))
                R.id.bottom_profile -> startActivity(Intent(this, HomeActivity::class.java))
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

        locationListener = LocationListener { location ->
            trackBoolean = sharedPreferences.getBoolean("trackBoolean", false)

            if (trackBoolean == false) {
                val userLocation = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                sharedPreferences.edit().putBoolean("trackBoolean", true).apply()
            }
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                Snackbar.make(binding.root, "Permission needed for location", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission") {
                    permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            } else {
                permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else { //permission granted
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0F, locationListener)
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastLocation != null) {
                val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15f))
            }
            mMap.isMyLocationEnabled = true
        }
    }

    private fun addPharmacyMarkers(pharmaciesJson: String) {
        val jsonObj = JSONObject(pharmaciesJson)
        val pharmaciesArray = jsonObj.getJSONArray("result")

        for (i in 0 until pharmaciesArray.length()) {
            val pharmacy = pharmaciesArray.getJSONObject(i)
            val locString = pharmacy.getString("loc") // "loc" is in format "lat,lng"
            val parts = locString.split(",")
            val lat = parts[0].toDouble() // Convert latitude to Double
            val lng = parts[1].toDouble() // Convert longitude to Double
            val pharmacyName = pharmacy.getString("name") // Pharmacy name
            val address = pharmacy.getString("address") // Pharmacy address
            runOnUiThread {
                val markerOptions = MarkerOptions()
                    .position(LatLng(lat, lng))
                    .title(pharmacyName)
                    .snippet(address) // You can add the address as a snippet
                mMap.addMarker(markerOptions)
            }
        }
    }

    private fun registerLauncher() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0,
                        0F,
                        locationListener
                    )
                    val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (lastLocation != null) {
                        val lastUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15f))
                    }
                    mMap.isMyLocationEnabled = true
                }
            } else {
                Toast.makeText(this@PharmaciesActivity, "Permission needed!", Toast.LENGTH_LONG).show()
            }
        }
    }


    // Updated fetchDutyPharmacies method
    private fun fetchDutyPharmacies(city: String, district: String? = null) {
        val client = OkHttpClient()

        var url = "https://api.collectapi.com/health/dutyPharmacy?il=$city"
        if (district != null) {
            url += "&ilce=$district"
        }

        val request = Request.Builder()
            .url(url)
            .header("authorization", "apikey 00MjE7zGQB3LRFcbJHUe3X:6z7xPEB4gwe1z7mZ2oAtV8")
            .header("content-type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PharmaciesActivity", "Error fetching pharmacies", e)
                runOnUiThread {
                    Toast.makeText(applicationContext, "Error fetching pharmacies: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.w("PharmaciesActivity", "Response not successful: ${response.message}")
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Response not successful: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                val responseData = response.body?.string()
                Log.d("PharmaciesActivity", "Response: $responseData")
                runOnUiThread {
                    if (responseData != null) {
                        Toast.makeText(applicationContext, "Pharmacies fetched successfully!", Toast.LENGTH_LONG).show()
                        addPharmacyMarkers(responseData)  // Call to place markers
                    }
                }
            }
        })
    }


}

