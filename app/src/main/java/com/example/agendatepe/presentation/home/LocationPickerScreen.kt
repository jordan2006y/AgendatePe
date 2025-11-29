package com.example.agendatepe.presentation.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Address
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.agendatepe.R
import com.example.agendatepe.ui.theme.*
import com.example.agendatepe.utils.LocationHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Helper para convertir el Vector XML a un Icono de Mapa
fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
    vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
    val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Composable
fun LocationPickerScreen(
    onLocationSelected: (Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // Centro inicial (Lima)
    val defaultLocation = LatLng(-12.0464, -77.0428)

    // LÍMITES DE LIMA (Restringe el deslizamiento)
    val limaBounds = remember {
        LatLngBounds(
            LatLng(-12.36, -77.16), // Sur-Oeste
            LatLng(-11.80, -76.80)  // Norte-Este
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    // Configuración visual del mapa (Oscuro y Limitado)
    val mapProperties = remember {
        MapProperties(
            isMyLocationEnabled = true,
            latLngBoundsForCameraTarget = limaBounds,
            minZoomPreference = 11.5f, // Zoom mínimo para no ver fuera de Lima
            maxZoomPreference = 20f,
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, rotationGesturesEnabled = true)
    }

    var currentCenter by remember { mutableStateOf(defaultLocation) }
    var addressText by remember { mutableStateOf("Mueve el mapa...") }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Address>>(emptyList()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        searchJob?.cancel()
        if (query.isBlank()) { searchResults = emptyList(); return }
        searchJob = scope.launch { delay(500); searchResults = LocationHelper.searchPlaces(context, query) }
    }

    fun selectLocation(location: Address) {
        keyboardController?.hide()
        searchQuery = ""
        searchResults = emptyList()
        scope.launch {
            val latLng = LatLng(location.latitude, location.longitude)
            if (limaBounds.contains(latLng)) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 16f), 1000)
            } else {
                // Si busca algo fuera de los límites, animamos cerca pero respetando el borde
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 16f), 1000)
            }
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            currentCenter = cameraPositionState.position.target
            scope.launch {
                val address = LocationHelper.getAddressFromCoordinates(context, currentCenter.latitude, currentCenter.longitude)
                addressText = address.ifEmpty { "Ubicación sin nombre" }
            }
        } else {
            addressText = "Buscando..."
        }
    }

    // --- UI ---
    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings
        )

        // Buscador Flotante (Estilo Glass/Dark)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { onSearchQueryChanged(it) },
                placeholder = { Text("Buscar calle o distrito...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Azul,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                trailingIcon = { Icon(Icons.Default.Search, null, tint = Azul) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
            )

            if (searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                        .background(DarkSurface.copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                        .heightIn(max = 250.dp)
                ) {
                    items(searchResults) { address ->
                        DropdownMenuItem(
                            text = { Text(address.featureName ?: "", color = Color.White) },
                            onClick = { selectLocation(address) }
                        )
                    }
                }
            }
        }

        // --- PINCHITO PERSONALIZADO AL CENTRO ---
        // Usamos Icon apuntando al drawable que creamos arriba
        Icon(
            painter = painterResource(id = R.drawable.ic_custom_pin),
            contentDescription = null,
            tint = Color.Unspecified, // IMPORTANTE: Para mantener los colores originales del XML
            modifier = Modifier
                .size(54.dp) // Tamaño grandecito para que se vea bien
                .align(Alignment.Center)
                .offset(y = (-27).dp) // Subir la mitad de la altura para que la punta toque el centro
        )

        // Tarjeta Inferior de Confirmación (Estilo Glass)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .glassEffect(backgroundColor = DarkSurface.copy(alpha = 0.9f), shape = RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ubicación Seleccionada", fontSize = 12.sp, color = Azul, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = addressText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onLocationSelected(currentCenter.latitude, currentCenter.longitude, addressText) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).shadow(12.dp, RoundedCornerShape(25.dp), spotColor = Azul),
                    colors = ButtonDefaults.buttonColors(containerColor = Azul),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Confirmar Ubicación", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}