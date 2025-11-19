package com.example.agendatepe.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object LocationHelper {

    // LÍMITES DE LIMA Y CALLAO (Ajustados para mayor precisión)
    private const val LOWER_LEFT_LAT = -12.36 // Sur (Lurín aprox)
    private const val LOWER_LEFT_LNG = -77.20 // Oeste (Callao/Mar)
    private const val UPPER_RIGHT_LAT = -11.80 // Norte (Carabayllo aprox)
    private const val UPPER_RIGHT_LNG = -76.80 // Este (Chosica aprox)

    // 1. BUSCAR LUGARES (Optimizado para "Autocompletado Falso")
    suspend fun searchPlaces(context: Context, query: String): List<Address> {
        return withContext(Dispatchers.IO) {
            if (query.length < 3) return@withContext emptyList() // Espera al menos 3 letras para no saturar

            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                // TRUCO: No agregamos ", Lima" al texto forzosamente,
                // confiamos en el Bounding Box (Caja de coordenadas) para que priorice Lima.
                // Esto permite que búsquedas cortas como "Santa" funcionen mejor.

                val results = geocoder.getFromLocationName(
                    query,
                    20, // TRUCO: Pedimos 20 resultados para tener variedad (Santa Anita, Santa Clara, etc)
                    LOWER_LEFT_LAT, LOWER_LEFT_LNG,
                    UPPER_RIGHT_LAT, UPPER_RIGHT_LNG
                )

                // Filtramos basura (resultados sin nombre)
                return@withContext results?.filter {
                    it.hasLatitude() && it.hasLongitude() && (it.featureName != null || it.thoroughfare != null)
                } ?: emptyList()

            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // 2. OBTENER DIRECCIÓN LEGIBLE (Igual que antes, funciona bien)
    suspend fun getAddressFromCoordinates(context: Context, lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val list = geocoder.getFromLocation(lat, lng, 1)

                if (!list.isNullOrEmpty()) {
                    val address = list[0]
                    val calle = address.thoroughfare
                    val numero = address.subThoroughfare
                    val lugar = address.featureName
                    val distrito = address.subLocality ?: address.locality // Distrito

                    val parte1 = if (!calle.isNullOrEmpty()) "$calle ${numero ?: ""}".trim() else lugar ?: ""
                    val parte2 = distrito ?: ""

                    if (parte1.isNotEmpty() && parte2.isNotEmpty() && parte1 != parte2) "$parte1, $parte2"
                    else if (parte1.isNotEmpty()) parte1
                    else parte2
                } else {
                    ""
                }
            } catch (e: Exception) {
                ""
            }
        }
    }
}