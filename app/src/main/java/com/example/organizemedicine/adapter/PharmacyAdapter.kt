package com.example.organizemedicine.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.organizemedicine.databinding.RecyclerRowPharmacyBinding
import com.example.organizemedicine.model.Pharmacy


class PharmacyAdapter (private val pharmacyList: ArrayList<Pharmacy>) : RecyclerView.Adapter<PharmacyAdapter.PharmacyHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PharmacyHolder {
        val binding = RecyclerRowPharmacyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PharmacyHolder(binding)

    }


    override fun onBindViewHolder(holder: PharmacyHolder, position: Int) {
        val pharmacy = pharmacyList[position]
        holder.bind(pharmacy)


    }

    override fun getItemCount(): Int {
        return pharmacyList.size
    }




    class PharmacyHolder(private val binding: RecyclerRowPharmacyBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pharmacy: Pharmacy) {
            binding.tvPharmacyName.text = pharmacy.name
            binding.tvPharmacyDistrict.text = pharmacy.dist
            binding.tvPharmacyAddress.text = pharmacy.address
            binding.tvPharmacyPhone.text = pharmacy.phone

            binding.btnLocation.setOnClickListener {
                openMaps(binding.root.context, pharmacy.loc)
            }
        }

        private fun openMaps(context: Context, loc: String) {
            val locParts = loc.split(",") // Split the loc string into latitude and longitude
            val lat = locParts[0].toDouble() // Convert latitude to Double
            val lng = locParts[1].toDouble() // Convert longitude to Double

            val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            }
        }

    }
}

