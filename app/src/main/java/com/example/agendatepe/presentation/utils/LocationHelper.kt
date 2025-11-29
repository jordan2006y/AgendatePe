package com.example.agendatepe.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Address
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object LocationHelper {

    // LÍMITES DE LIMA Y CALLAO
    private const val LOWER_LEFT_LAT = -12.36
    private const val LOWER_LEFT_LNG = -77.20
    private const val UPPER_RIGHT_LAT = -11.80
    private const val UPPER_RIGHT_LNG = -76.80

    // 1. BUSCAR LUGARES
    suspend fun searchPlaces(context: Context, query: String): List<Address> {
        return withContext(Dispatchers.IO) {
            if (query.length < 3) return@withContext emptyList()
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val results = geocoder.getFromLocationName(
                    query,
                    20,
                    LOWER_LEFT_LAT, LOWER_LEFT_LNG,
                    UPPER_RIGHT_LAT, UPPER_RIGHT_LNG
                )
                return@withContext results?.filter {
                    it.hasLatitude() && it.hasLongitude() && (it.featureName != null || it.thoroughfare != null)
                } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // 2. OBTENER DIRECCIÓN LEGIBLE
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
                    val distrito = address.subLocality ?: address.locality
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

    // 3. CONVERTIR VECTOR A BITMAP (PARA EL PIN DEL MAPA)
    fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
        vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}