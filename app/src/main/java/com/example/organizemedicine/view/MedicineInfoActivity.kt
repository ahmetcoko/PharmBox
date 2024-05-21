package com.example.organizemedicine.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.organizemedicine.R
import com.example.organizemedicine.databinding.ActivityMedicineInfoBinding

class MedicineInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMedicineInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicineInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val medicineName = intent.getStringExtra("medicine_name")
        binding.titleTextView.text = medicineName?.uppercase()

        binding.apply {
            bottomMenu.setItemSelected(R.id.bottom_search)
            bottomMenu.setOnItemSelectedListener {
                if (it == R.id.bottom_upload) {
                    startActivity(Intent(this@MedicineInfoActivity, PostUploadActivity::class.java))
                } else if (it == R.id.bottom_profile) {
                    startActivity(Intent(this@MedicineInfoActivity, HomeActivity::class.java))
                } else if (it == R.id.bottom_home) {
                    startActivity(Intent(this@MedicineInfoActivity, MedicineFeedActivity::class.java))
                }
            }
        }




    }
}