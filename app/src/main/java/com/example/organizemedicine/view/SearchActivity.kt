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


        loadMedicines()



        loadDefaultMedicines()

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

    private fun loadDefaultMedicines() {
        val defaultMedicines = listOf(
            "PAROL 120 MG/5 ML 150 ML ORAL SUSPANSIYON",
            "MAJEZIK DUO 100 MG/8 MG 14 FILM KAPLI TABLET",
            "TYLOL 500 MG 20 TABLET",
            "NEXIUM 20 MG ENTERIK KAPLI 28 PELLET TABLET",
            "NAPROSYN %10 50 GR JEL",
            "ASPIRIN 100 MG 20 TABLET",
            "APRANAX 275 MG 20 FILM KAPLITABLET",
            "AUGMENTIN BID 625 MG 14 FILMTABLET",
            "ZYRTEC 10 MG 20 FILM TABLET",
            "XANAX 1 MG 50 TABLET",
            "NUROFEN COLD FLU 24 TABLET",
            "VERMIDON 500/30 MG TABLET 30 TABLET",
            "GRIPIN 8 KAPSUL",
            "MORFIA 15 MG 30 TABLET",
            "MINOSET 500 MG 20 TABLET",
            "VASOSERC BID 24 MG 60 TABLET",
            "EUTHYROX 25 MCG 50 TABLET",
            "DICLOFLAM 50 MG KAPLI TABLET (20 KAPLI TABLET)",
            "BETASERC 24 MG 20 TABLET",
            "BERAZINC 220 40 KAPSUL"
        )
        addMedicinesToLayout(defaultMedicines)
    }

    private fun addMedicinesToLayout(medicines: List<String>) {
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
                text = medicine.uppercase()
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