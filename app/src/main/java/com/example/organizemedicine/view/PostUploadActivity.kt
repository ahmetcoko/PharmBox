package com.example.organizemedicine.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
    var selectedPicture : Uri? = null
    private lateinit var auth : FirebaseAuth
    private lateinit var firestore : FirebaseFirestore
    private lateinit var storage: FirebaseStorage

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
            }






        }





    }

    fun upload(view: View){
        val uuid = UUID.randomUUID()
        val imageName = "$uuid.jpg"
        val reference = storage.reference // reference artık bize firebase storage ekranını veriyor
        val imageReference = reference.child("images").child(imageName) // bir alt reference oluşturdun , bir üstü için parent kullanman lazım içine "images/images.jpg" yazarsan child olarak images klasörü açar içine images.jpg koyar

        if (selectedPicture != null){
            imageReference.putFile(selectedPicture!!).addOnSuccessListener{
                    taskSnapshot ->
                // Task completed successfully
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    val postMap  = hashMapOf<String, Any>()
                    if (auth.currentUser != null){
                        postMap["downloadUrl"] = downloadUrl
                        postMap["userEmail"] = auth.currentUser!!.email!!
                        postMap["comment"] = binding.commentText.text.toString()
                        postMap["date"] = Timestamp.now()
                        postMap["score"] = binding.ratingBar.rating // Capture the rating from the RatingBar

                        firestore.collection("Posts").add(postMap).addOnSuccessListener {
                            finish()
                        }.addOnFailureListener {
                            Toast.makeText(this@PostUploadActivity,it.localizedMessage,Toast.LENGTH_LONG).show()
                        }
                    }



                }.addOnFailureListener { exception ->
                    Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_LONG).show()
                }


            }.addOnFailureListener{
                Toast.makeText(this,it.localizedMessage,Toast.LENGTH_LONG).show()
            }
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