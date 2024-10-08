package com.example.organizemedicine.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
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
    private var medicinesList = mutableListOf<String>()
    private var filteredMedicines = mutableListOf<String>()
    private var searchJob: Job? = null
    private var searchHandler = Handler(Looper.getMainLooper())
    private val debouncePeriod: Long = 1
    private var medicinesSet = mutableSetOf<String>()
    private val trie = Trie()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostUploadBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val appCheckConfig = SafetyNetAppCheckProviderFactory.getInstance()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckConfig)
        registerLauncher()

        loadMedicines()

        auth = Firebase.auth
        firestore = Firebase.firestore
        storage = Firebase.storage


        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                binding.imageView.setImageBitmap(imageBitmap)

                
                val savedImageUri = saveBitmapToCache(imageBitmap)
                selectedPicture = savedImageUri
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

        binding.medicineSearchTxt.addTextChangedListener {
            searchHandler.removeCallbacksAndMessages(null)
            val searchText = it.toString().lowercase()
            searchHandler.postDelayed({
                filterMedicines(searchText)
            },debouncePeriod)
        }

        initSearchFunctionality()
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    private fun loadMedicines() {
        val inputStream = assets.open("medicines.csv")
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.useLines { lines ->
            lines.forEach { trie.insert(it.trim().lowercase()) }
        }
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        
        val file = File(externalCacheDir, "${UUID.randomUUID()}.jpg")
        val fileOutputStream = FileOutputStream(file)

        
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)

        
        fileOutputStream.close()

        
        return Uri.fromFile(file)
    }

    fun deletePhoto(view: View) {
        binding.imageView.setImageResource(R.drawable.select_image)
    }

    private fun initSearchFunctionality() {
        binding.medicineSearchTxt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterMedicines(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private fun filterMedicines(query: String) {
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.Default).launch {
            delay(300)
            val results = trie.wordsWithPrefix(query.lowercase())
            withContext(Dispatchers.Main) {
                updateUI(results)
            }
        }
    }

    private fun updateUI(medicines: List<String>) {
        binding.medicinesLayout.removeAllViews()
        medicines.forEach { medicine ->
            val textView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(8, 8, 8, 8) }
                text = medicine.uppercase()
                background = ContextCompat.getDrawable(context, R.drawable.fragment_background)
                setPadding(16, 16, 16, 16)
                gravity = Gravity.CENTER
                setOnClickListener{
                    binding.pickedMedicineTxt.text = medicine.uppercase()
                }
            }
            binding.medicinesLayout.addView(textView)
        }
    }

    fun upload(view: View) {
        val uuid = UUID.randomUUID()
        val imageName = "$uuid.jpg"
        val reference = storage.reference
        val imageReference = reference.child("images").child(imageName)

        if (selectedPicture != null) {
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
            
            savePostToFirestore(null)
        }
    }

    private fun savePostToFirestore(downloadUrl: String?) {

        firestore.collection("Users").document(auth.currentUser?.uid!!).get().addOnSuccessListener { document ->
            val name = document.getString("firstName")
            val surname = document.getString("lastName")
            val userName = document.getString("username")
            val fullName = "$name $surname"

            val postMap = hashMapOf(
                "userEmail" to (auth.currentUser?.email ?: "No Email"),
                "username" to userName,
                "fullName" to fullName,
                "comment" to binding.commentText.text.toString(),
                "date" to Timestamp.now(),
                "likedBy" to listOf<String>(),
                "medicineName" to binding.pickedMedicineTxt.text.toString(),
                "commentsCount" to 0,
            )

            downloadUrl?.let {
                postMap["downloadUrl"] = it
            }

            firestore.collection("Posts").add(postMap).addOnSuccessListener { documentReference ->
                val postId = documentReference.id
                documentReference.update("postId", postId)
                navigateToMedicineFeedActivity()
                Toast.makeText(this, "You will be navigated to Feed Screen", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Log.e("PostUploadActivity", "Failed to add document: ${e.localizedMessage}")
                Toast.makeText(this, "Error saving post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("PostUploadActivity", "Failed to retrieve user: ${e.localizedMessage}")
            Toast.makeText(this, "Error retrieving user: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMedicineFeedActivity() {
        val intent = Intent(this@PostUploadActivity, MedicineFeedActivity::class.java)
        startActivity(intent)
        finish()
    }





    fun selectImage(view: View) {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission denied for gallery" , Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                    
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }.show()
            }else{
               
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        }else{
            val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            
            activityResultLauncher.launch(intentToGallery)


        }

    }

    private fun registerLauncher(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            if (result.resultCode == RESULT_OK){
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    selectedPicture = intentFromResult.data

                    selectedPicture?.let {
                        binding.imageView.setImageURI(it)
                    }

                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result ->
            if (result){

                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)

            }else{
                Toast.makeText(this@PostUploadActivity,"Permission needed!",Toast.LENGTH_LONG).show()

            }
        }

    }
}
