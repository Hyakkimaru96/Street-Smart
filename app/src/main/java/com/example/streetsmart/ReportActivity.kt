package com.example.streetsmart

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.content.IntentSender
import android.location.LocationManager
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ReportActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var btnTakePhoto: Button
    private lateinit var btnUploadPhoto: Button
    private lateinit var btnSubmit: Button
    private lateinit var editDescription: EditText
    private lateinit var locationText: TextView

    private var imageUri: Uri? = null
    private var imageFile: File? = null

    private val CAMERA_REQUEST_CODE = 100
    private val GALLERY_REQUEST_CODE = 101
    private val PERMISSION_REQUEST_CODE = 102
    private val CAMERA_AND_STORAGE_PERMISSION_REQUEST_CODE = 103


    private val locationViewModel: LocationViewModel by viewModels()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        imagePreview = findViewById(R.id.imagePreview)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto)
        btnSubmit = findViewById(R.id.btnSubmit)
        editDescription = findViewById(R.id.editDescription)
        locationText = findViewById(R.id.locationText)

        checkPermissionsAndFetchLocation()

        btnTakePhoto.setOnClickListener { openCamera() }
        btnUploadPhoto.setOnClickListener { openGallery() }

        btnSubmit.setOnClickListener {
            val description = editDescription.text.toString()
            val location = locationText.text.toString()

            if (description.isBlank()) {
                Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadImageToCloudinary { imageUrl ->
                val report = Report(
                    description = description,
                    location = location,
                    imageUrl = imageUrl,
                    userId = auth.currentUser?.uid ?: "",
                    upvotes = 0,
                    downvotes = 0,
                    status = 0,
                    userVoteStatus = 0
                )

                firestore.collection("reports")
                    .add(report)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Report submitted!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to submit report", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        locationViewModel.locationText.observe(this) { text ->
            locationText.text = text
        }
    }

    private fun checkPermissionsAndFetchLocation() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            // Check if GPS is turned on
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

            val client: SettingsClient = LocationServices.getSettingsClient(this)
            val task = client.checkLocationSettings(builder.build())

            task.addOnSuccessListener {
                // All location settings are satisfied, fetch location
                locationViewModel.getLocation(this)
            }

            task.addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        exception.startResolutionForResult(this, 2001)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        sendEx.printStackTrace()
                    }
                } else {
                    Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            locationViewModel.getLocation(this)
        } else if (requestCode == CAMERA_AND_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted. Try again.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Camera or storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun openCamera() {
        if (hasCameraAndStoragePermissions()) {
            imageFile = createImageFile()
            imageUri = FileProvider.getUriForFile(this, "$packageName.provider", imageFile!!)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        } else {
            requestCameraAndStoragePermissions()
        }
    }

    private fun openGallery() {
        if (hasCameraAndStoragePermissions()) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, GALLERY_REQUEST_CODE)
        } else {
            requestCameraAndStoragePermissions()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2001) {
            if (resultCode == Activity.RESULT_OK) {
                // User enabled GPS
                locationViewModel.getLocation(this)
            } else {
                Toast.makeText(this, "Location services are required to submit a report", Toast.LENGTH_LONG).show()
            }
        }

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> imagePreview.setImageURI(imageUri)
                GALLERY_REQUEST_CODE -> {
                    imageUri = data?.data
                    imageFile = imageUri?.let { uri ->
                        val inputStream = contentResolver.openInputStream(uri)
                        val tempFile = createImageFile()
                        inputStream?.use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile
                    }
                    imagePreview.setImageURI(imageUri)
                }
            }
        }
    }


    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timestamp.jpg"
        val storageDir = cacheDir
        return File(storageDir, fileName)
    }

    private fun hasCameraAndStoragePermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        return cameraPermission == PackageManager.PERMISSION_GRANTED &&
                storagePermission == PackageManager.PERMISSION_GRANTED
    }


    private fun requestCameraAndStoragePermissions() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), CAMERA_AND_STORAGE_PERMISSION_REQUEST_CODE)
    }



    private fun uploadImageToCloudinary(onUploaded: (String) -> Unit) {
        val file = imageFile
        if (file == null) {
            onUploaded("") // No image, proceed anyway
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", "streetsmart")
            .addFormDataPart("folder", "reports")
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/dvsawqinf/image/upload")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ReportActivity, "Image upload failed", Toast.LENGTH_SHORT).show()
                    onUploaded("")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                val json = JSONObject(responseData ?: "")
                val imageUrl = json.optString("secure_url", "")
                runOnUiThread {
                    onUploaded(imageUrl)
                }
            }
        })
    }
}