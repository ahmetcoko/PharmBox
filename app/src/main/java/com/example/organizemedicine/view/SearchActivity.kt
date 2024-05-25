package com.example.organizemedicine.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.example.organizemedicine.R
import com.example.organizemedicine.databinding.ActivitySearchBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader


class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private var medicinesList = mutableListOf<String>()
    private var filteredMedicines = mutableListOf<String>()
    private var searchJob: Job? = null
    private var searchHandler = Handler(Looper.getMainLooper())
    private val debouncePeriod: Long = 1
    private var medicinesSet = mutableSetOf<String>()
    private val trie = Trie()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load medicines from the CSV file
        loadMedicines()

        filterMedicines("A")

        binding.searchText.addTextChangedListener {
            searchHandler.removeCallbacksAndMessages(null)
            val searchText = it.toString().lowercase()
            searchHandler.postDelayed({
                filterMedicines(searchText)
            },debouncePeriod)
        }

        binding.apply {
            bottomMenu.setItemSelected(R.id.bottom_search)
            bottomMenu.setOnItemSelectedListener {
                if (it == R.id.bottom_upload) {
                    startActivity(Intent(this@SearchActivity, PostUploadActivity::class.java))
                } else if (it == R.id.bottom_profile) {
                    startActivity(Intent(this@SearchActivity, HomeActivity::class.java))
                } else if (it == R.id.bottom_home) {
                    startActivity(Intent(this@SearchActivity, MedicineFeedActivity::class.java))
                }else if (it == R.id.bottom_Pharmacies) {
                    startActivity(Intent(this@SearchActivity, PharmaciesActivity::class.java))
                }
            }
        }
    }

    private fun loadMedicines() {
        val inputStream = assets.open("medicines.csv")
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.useLines { lines ->
            lines.forEach { trie.insert(it.trim().lowercase()) }
        }
    }

    private fun filterMedicines(query: String) {
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.Default).launch {
            delay(300)  // Wait for 300ms before starting the search
            val results = trie.wordsWithPrefix(query.lowercase())
            withContext(Dispatchers.Main) {
                updateUI(results)
            }
        }
    }



    private fun updateUI(medicines: List<String>) {
        binding.itemsLayout.removeAllViews()
        medicines.forEach { medicine ->
            val textView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(8, 8, 8, 8) }
                text = medicine.uppercase()  // Convert the medicine name to uppercase
                background = ContextCompat.getDrawable(context, R.drawable.fragment_background)
                setPadding(16, 16, 16, 16)
                gravity = Gravity.CENTER
                setOnClickListener {
                    val intent = Intent(this@SearchActivity, MedicineInfoActivity::class.java)
                    intent.putExtra("medicine_name", medicine)
                    startActivity(intent)
                }
            }
            binding.itemsLayout.addView(textView)
        }
    }
}