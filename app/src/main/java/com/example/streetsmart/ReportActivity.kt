package com.example.streetsmart

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
            locationViewModel.getLocation(this)
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
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        imageFile = createImageFile()
        imageUri = FileProvider.getUriForFile(this, "$packageName.provider", imageFile!!)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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