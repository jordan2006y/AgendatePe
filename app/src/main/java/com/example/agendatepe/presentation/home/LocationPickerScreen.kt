package com.example.agendatepe.presentation.home

import android.location.Address
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendatepe.ui.theme.*
import com.example.agendatepe.utils.LocationHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LocationPickerScreen(
    onLocationSelected: (Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val defaultLocation = LatLng(-12.0464, -77.0428)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    var currentCenter by remember { mutableStateOf(defaultLocation) }
    var addressText by remember { mutableStateOf("Mueve el mapa para ver la dirección...") }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Address>>(emptyList()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        searchJob?.cancel()
        if (query.isBlank()) {
            searchResults = emptyList()
            return
        }
        searchJob = scope.launch {
            delay(300)
            searchResults = LocationHelper.searchPlaces(context, query)
        }
    }

    fun selectLocation(location: Address) {
        keyboardController?.hide()
        searchQuery = ""
        searchResults = emptyList()
        scope.launch {
            val latLng = LatLng(location.latitude, location.longitude)
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 16f), 1000)
            val street = location.thoroughfare ?: ""
            val number = location.subThoroughfare ?: ""
            val district = location.subLocality ?: location.locality ?: ""
            val fullAddress = if (street.isNotEmpty()) "$street $number, $district" else district
            addressText = fullAddress.ifEmpty { location.featureName ?: "Ubicación seleccionada" }
        }
    }

    fun searchLocationManual() {
        if (searchQuery.isBlank()) return
        keyboardController?.hide()
        scope.launch {
            val results = LocationHelper.searchPlaces(context, searchQuery)
            if (results.isNotEmpty()) {
                selectLocation(results[0])
            }
        }
    }

    fun updateAddressFromMap(latLng: LatLng) {
        scope.launch {
            val address = LocationHelper.getAddressFromCoordinates(context, latLng.latitude, latLng.longitude)
            addressText = address.ifEmpty { "Ubicación sin nombre exacto" }
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            currentCenter = cameraPositionState.position.target
            val lat = currentCenter.latitude
            val lng = currentCenter.longitude

            // LÍMITES DE LIMA (Aprox)
            if (lat > -11.5 || lat < -12.6 || lng > -76.5 || lng < -77.3) {
                Toast.makeText(context, "⚠️ Estás saliendo de la zona de cobertura (Lima)", Toast.LENGTH_SHORT).show()
            }

            updateAddressFromMap(currentCenter)
        } else {
            addressText = "Buscando..."
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { onSearchQueryChanged(it) },
                placeholder = { Text("Escribe calle o distrito...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    cursorColor = Azul,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                    } else {
                        IconButton(onClick = { searchLocationManual() }) { Icon(Icons.Default.Search, null, tint = Azul) }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searchLocationManual() })
            )

            if (searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .shadow(8.dp, RoundedCornerShape(8.dp))
                        .heightIn(max = 300.dp)
                ) {
                    items(searchResults) { address ->
                        val title = address.featureName ?: address.thoroughfare ?: "Ubicación"
                        val district = address.subLocality ?: address.locality ?: ""
                        val city = address.adminArea ?: ""
                        val subtitle = listOf(district, city).filter { it.isNotEmpty() && it != title }.joinToString(", ")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectLocation(address) }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = if (address.featureName != null) Icons.Default.Place else Icons.Default.Search
                            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                                if (subtitle.isNotBlank()) {
                                    Text(subtitle, fontSize = 13.sp, color = Color.Gray)
                                }
                            }
                        }
                        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }

        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier.size(48.dp).align(Alignment.Center).offset(y = (-24).dp)
        )

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            color = Color.White,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ubicación Seleccionada:", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = addressText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onLocationSelected(currentCenter.latitude, currentCenter.longitude, addressText) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Azul),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar esta Ubicación", color = Color.White)
                }
            }
        }
    }
}