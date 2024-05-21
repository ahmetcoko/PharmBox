package com.example.organizemedicine.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.organizemedicine.R
import com.example.organizemedicine.databinding.ActivityPostUploadBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import java.util.UUID

class PostUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostUploadBinding
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private var selectedPicture : Uri? = null
    private lateinit var auth : FirebaseAuth
    private lateinit var firestore : FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostUploadBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val appCheckConfig = SafetyNetAppCheckProviderFactory.getInstance()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckConfig)
        registerLauncher()

        auth = Firebase.auth
        firestore = Firebase.firestore
        storage = Firebase.storage

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                // Use the imageBitmap as needed
                // For example, you can set it to an ImageView
                binding.imageView.setImageBitmap(imageBitmap)
            }
        }

        binding.cameraBtn.setOnClickListener {
            openCamera()
        }

        binding.deleteBtn.setOnClickListener {
            deletePhoto(view)
        }

        binding.apply {
            bottomMenu.setItemSelected(R.id.bottom_upload)
            bottomMenu.setOnItemSelectedListener {
                if (it == R.id.bottom_home){
                    startActivity(Intent(this@PostUploadActivity,MedicineFeedActivity::class.java))
                }
                else if (it == R.id.bottom_search){
                    startActivity(Intent(this@PostUploadActivity,SearchActivity::class.java))
                }
                else if (it == R.id.bottom_profile){
                    startActivity(Intent(this@PostUploadActivity,HomeActivity::class.java))
                }
                else if (it == R.id.bottom_Pharmacies){
                    startActivity(Intent(this@PostUploadActivity,PharmaciesActivity::class.java))
                }
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    fun deletePhoto(view: View) {
        binding.imageView.setImageResource(R.drawable.select_image)
    }

    fun upload(view: View) {
        val uuid = UUID.randomUUID()
        val imageName = "$uuid.jpg"
        val reference = storage.reference
        val imageReference = reference.child("images").child(imageName)

        if (selectedPicture != null) {
            // Upload the selected picture
            imageReference.putFile(selectedPicture!!).addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    savePostToFirestore(downloadUrl)
                }.addOnFailureListener { exception ->
                    Log.e("PostUploadActivity", "Failed to get download URL: ${exception.localizedMessage}")
                    Toast.makeText(this, "Error getting download URL: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Log.e("PostUploadActivity", "Failed to upload file: ${it.localizedMessage}")
                Toast.makeText(this, "Error uploading image: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // If no picture is selected, use a null URL for the post
            savePostToFirestore(null)
        }
    }

    private fun savePostToFirestore(downloadUrl: String?) {
        val postMap = hashMapOf<String, Any>(
            "userEmail" to (auth.currentUser?.email ?: ""),
            "comment" to binding.commentText.text.toString(),
            "date" to Timestamp.now(),
            "likedBy" to listOf<String>()
        )

        // Add download URL only if it is not null
        downloadUrl?.let {
            postMap["downloadUrl"] = it
        }

        firestore.collection("Posts").add(postMap).addOnSuccessListener { documentReference ->
            val postId = documentReference.id  // Get the ID of the newly created document
            postMap["postId"] = postId  // Add the postId to the postMap
            documentReference.update("postId", postId)  // Update the document to include the postId
            // Navigate to MedicineFeedActivity
            val intent = Intent(this@PostUploadActivity, MedicineFeedActivity::class.java)
            startActivity(intent)
            finish()
        }.addOnFailureListener { e ->
            Log.e("PostUploadActivity", "Failed to add document: ${e.localizedMessage}")
            Toast.makeText(this, "Error saving post: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }




    fun selectImage(view: View) {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission denied for gallery" , Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                    //request permission
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }.show()
            }else{
                //request permission
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        }else{
            val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            //start activity fo result
            activityResultLauncher.launch(intentToGallery)


        }

    }

    private fun registerLauncher(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            if (result.resultCode == RESULT_OK){
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    selectedPicture = intentFromResult.data // fotonunun URI'ı(nerede tutulduğu)

                    selectedPicture?.let {
                        binding.imageView.setImageURI(it)
                    }

                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result ->
            if (result){
                //permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }else{//permission denied
                Toast.makeText(this@PostUploadActivity,"Permission needed!",Toast.LENGTH_LONG).show()

            }
        }

    }
}