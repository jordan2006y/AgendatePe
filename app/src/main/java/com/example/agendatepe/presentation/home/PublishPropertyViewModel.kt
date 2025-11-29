package com.example.agendatepe.presentation.home

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class PublishPropertyViewModel : ViewModel() {

    // --- CONTROL DE PASOS (WIZARD) ---
    // Lo movemos aquí para que no se reinicie al ir al mapa
    var currentStep by mutableIntStateOf(1)

    // --- DATOS DEL FORMULARIO ---
    var selectedCategory by mutableStateOf("Casa")
    var expandedCategory by mutableStateOf(false)

    var title by mutableStateOf("")
    var price by mutableStateOf("")
    var currency by mutableStateOf("S/")
    var description by mutableStateOf("")
    var area by mutableStateOf("")

    // Contadores
    var bedrooms by mutableIntStateOf(1)
    var bathrooms by mutableIntStateOf(1)

    // Características
    var hasPool by mutableStateOf(false)
    var hasGarage by mutableStateOf(false)
    var hasGarden by mutableStateOf(false)
    var isPetFriendly by mutableStateOf(false)
    var hasPapers by mutableStateOf(false)

    // Fotos
    var selectedImages by mutableStateOf<List<Uri>>(emptyList())
    var poolUri by mutableStateOf<Uri?>(null)
    var garageUri by mutableStateOf<Uri?>(null)
    var gardenUri by mutableStateOf<Uri?>(null)

    // Ubicación
    var address by mutableStateOf("")
    var lat by mutableDoubleStateOf(0.0)
    var lng by mutableDoubleStateOf(0.0)
    var locationStatus by mutableStateOf("Sin ubicación seleccionada")

    // --- LÓGICA DE NEGOCIO ---

    fun onCategoryChange(newCategory: String) {
        selectedCategory = newCategory
        val isTerreno = newCategory == "Terreno"
        val isOficina = newCategory == "Oficina"

        if (isTerreno) {
            bedrooms = 0; bathrooms = 0; hasPool = false; hasGarage = false
            hasGarden = false; isPetFriendly = false; poolUri = null
            garageUri = null; gardenUri = null
        } else if (isOficina) {
            hasPool = false; poolUri = null
            if (bedrooms == 0) bedrooms = 1
            if (bathrooms == 0) bathrooms = 1
        } else {
            if (bedrooms == 0) bedrooms = 1
            if (bathrooms == 0) bathrooms = 1
        }
    }

    fun updateLocation(latitude: Double, longitude: Double, addressText: String) {
        lat = latitude
        lng = longitude
        if (addressText.isNotEmpty() && addressText != "Buscando...") {
            address = addressText
        }
        locationStatus = "¡Ubicación exacta guardada!"
    }
}