package com.example.agendatepe.presentation.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.agendatepe.R
import com.example.agendatepe.ui.theme.*
import com.example.agendatepe.utils.LocationHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- MODELO ---
data class Propiedad(
    val id: String = "", val userId: String = "", val telefono: String = "", val categoria: String = "", val titulo: String = "", val precio: String = "", val moneda: String = "S/", val direccion: String = "", val descripcion: String = "", val area: String = "", val habitaciones: Int = 0, val banos: Int = 0, val imagenes: List<String> = emptyList(), val tipo: String = "", val lat: Double = 0.0, val lng: Double = 0.0, val tienePiscina: Boolean = false, val tieneCochera: Boolean = false, val tieneJardin: Boolean = false, val esPetFriendly: Boolean = false, val fotoPiscina: String = "", val fotoCochera: String = "", val fotoJardin: String = "", val distrito: String = "", val etiquetasEntorno: List<String> = emptyList(), val favoritos: List<String> = emptyList()
)

// --- EFECTOS VISUALES ---
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "")
    val translateAnim = transition.animateFloat(initialValue = 0f, targetValue = 1000f, animationSpec = infiniteRepeatable(tween(durationMillis = 1200, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "")
    val brush = Brush.linearGradient(colors = listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.1f), Color.Gray.copy(alpha = 0.3f)), start = Offset.Zero, end = Offset(x = translateAnim.value, y = translateAnim.value))
    this.background(brush)
}

fun Modifier.glassEffect(backgroundColor: Color = Azul.copy(alpha = 0.85f), borderColor: Color = Color.White.copy(alpha = 0.3f), shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)) = composed {
    this.background(Brush.verticalGradient(listOf(backgroundColor, backgroundColor.copy(alpha = 0.95f))), shape).border(1.dp, Brush.linearGradient(listOf(borderColor, borderColor.copy(alpha = 0.1f))), shape).shadow(8.dp, shape, ambientColor = backgroundColor, spotColor = backgroundColor)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    navigateToPublish: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = Firebase.auth
    val currentUser = auth.currentUser
    val haptic = LocalHapticFeedback.current

    // TEMA POR DEFECTO: OSCURO
    var isDarkTheme by remember { mutableStateOf(true) }

    var isMapView by remember { mutableStateOf(false) }
    var isLoadingData by remember { mutableStateOf(true) }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- CONFIGURACIÓN MAPA ---
    val lima = LatLng(-12.0464, -77.0428)
    val limaBounds = remember { LatLngBounds(LatLng(-12.36, -77.16), LatLng(-11.80, -76.80)) }
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(lima, 12f) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    var mapProperties by remember {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = false,
                latLngBoundsForCameraTarget = limaBounds,
                minZoomPreference = 11.5f,
                maxZoomPreference = 20f,
            )
        )
    }
    var mapUiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)) }

    // Filtros
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterSelected by remember { mutableStateOf("Comprar") }
    var filterNearMe by remember { mutableStateOf(false) }
    var selectedDistrito by remember { mutableStateOf("Todos") }
    var minPrecio by remember { mutableStateOf("") }
    var maxPrecio by remember { mutableStateOf("") }
    var minHabitaciones by remember { mutableStateOf(0) }
    val availableTags = listOf("Zona Segura", "Cerca a Parque", "Vista al Mar", "Zona Comercial")
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

    // Menús y Dialogos
    var showProfileMenu by remember { mutableStateOf(false) }
    var showPublishMenu by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedProperty by remember { mutableStateOf<Propiedad?>(null) }
    var propiedadesList by remember { mutableStateOf<List<Propiedad>>(emptyList()) }
    var zoomImageSrc by remember { mutableStateOf<String?>(null) }
    var userImage by remember { mutableStateOf("") }

    // Funciones Helper
    fun checkGPSEnabled(): Boolean { val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager; return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }
    fun requestEnableGPS() { Toast.makeText(context, "Activa tu GPS para ver propiedades cercanas.", Toast.LENGTH_LONG).show(); context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
    fun onSearch(query: String) { searchQuery = query; searchJob?.cancel(); if (query.isBlank()) { searchResults = emptyList(); return }; searchJob = scope.launch { delay(800); searchResults = LocationHelper.searchPlaces(context, query) } }

    fun toggleFavorite(propiedad: Propiedad) {
        if (currentUser == null) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val docRef = FirebaseFirestore.getInstance().collection("propiedades").document(propiedad.id)
        if (propiedad.favoritos.contains(currentUser.uid)) { docRef.update("favoritos", FieldValue.arrayRemove(currentUser.uid)) } else { docRef.update("favoritos", FieldValue.arrayUnion(currentUser.uid)) }
    }

    // Carga de datos
    LaunchedEffect(Unit) {
        if (currentUser != null) FirebaseFirestore.getInstance().collection("users").document(currentUser.uid).get().addOnSuccessListener { document -> userImage = document.getString("image") ?: "" }
        FirebaseFirestore.getInstance().collection("propiedades").addSnapshotListener { value, _ ->
            if (value != null) {
                propiedadesList = value.documents.mapNotNull { doc ->
                    try {
                        val rawList = doc.get("imagenes") as? List<*>
                        val imgs = if (rawList != null) rawList.map { it.toString() } else listOf(doc.getString("imagen") ?: "")
                        val tags = (doc.get("etiquetasEntorno") as? List<*>)?.map { it.toString() } ?: emptyList()
                        val favs = (doc.get("favoritos") as? List<*>)?.map { it.toString() } ?: emptyList()
                        Propiedad(id = doc.id, userId = doc.getString("userId")?:"", telefono = doc.getString("telefono")?:"", categoria = doc.getString("categoria")?:"Inmueble", titulo = doc.getString("titulo")?:"", precio = doc.getString("precio")?:"", moneda = doc.getString("moneda")?:"S/", direccion = doc.getString("direccion")?:"", descripcion = doc.getString("descripcion")?:"", area = doc.getString("area")?:"", habitaciones = (doc.getLong("habitaciones")?:0).toInt(), banos = (doc.getLong("banos")?:0).toInt(), imagenes = imgs, tipo = doc.getString("tipo")?:"Venta", lat = doc.getDouble("lat")?:0.0, lng = doc.getDouble("lng")?:0.0, tienePiscina = doc.getBoolean("tienePiscina")?:false, tieneCochera = doc.getBoolean("tieneCochera")?:false, tieneJardin = doc.getBoolean("tieneJardin")?:false, esPetFriendly = doc.getBoolean("esPetFriendly")?:false, fotoPiscina = doc.getString("fotoPiscina")?:"", fotoCochera = doc.getString("fotoCochera")?:"", fotoJardin = doc.getString("fotoJardin")?:"", distrito = doc.getString("distrito")?:"", etiquetasEntorno = tags, favoritos = favs)
                    } catch (e: Exception) { null }
                }
                isLoadingData = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { permissions -> hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true; if (hasLocationPermission) { mapProperties = mapProperties.copy(isMyLocationEnabled = true); mapUiSettings = mapUiSettings.copy(myLocationButtonEnabled = true); try { fusedLocationClient.lastLocation.addOnSuccessListener { location -> location?.let { userLocation = LatLng(it.latitude, it.longitude) } } } catch (e: Exception) {} } }
    LaunchedEffect(Unit) { if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) { hasLocationPermission = true; mapProperties = mapProperties.copy(isMyLocationEnabled = true); mapUiSettings = mapUiSettings.copy(myLocationButtonEnabled = true); fusedLocationClient.lastLocation.addOnSuccessListener { location -> location?.let { userLocation = LatLng(it.latitude, it.longitude) } } } }

    if (zoomImageSrc != null) ZoomableImageDialog(imageUrl = zoomImageSrc!!, onDismiss = { zoomImageSrc = null })
    if (showLogoutDialog) { AlertDialog(onDismissRequest = { showLogoutDialog = false }, title = { Text("¿Cerrar Sesión?") }, text = { Text("¿Estás seguro de que quieres salir?") }, confirmButton = { Button(onClick = { showLogoutDialog = false; onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Cerrar Sesión", color = Color.White) } }, dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar", color = MaterialTheme.colorScheme.onBackground) } }, containerColor = MaterialTheme.colorScheme.surface) }

    AgendatePeTheme(darkTheme = isDarkTheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                Box(modifier = Modifier.padding(bottom = 80.dp).glassEffect(backgroundColor = Azul, shape = CircleShape).clickable { isMapView = !isMapView }.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = if (isMapView) Icons.Default.List else Icons.Default.Map, contentDescription = null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text(text = if (isMapView) "Ver Lista" else "Ver Mapa", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

                val tipoFiltro = if (filterSelected == "Comprar") "Venta" else "Alquiler"
                val listaFiltrada = propiedadesList.filter { prop ->
                    val matchTipo = prop.tipo == tipoFiltro
                    val matchDistrito = selectedDistrito == "Todos" || prop.direccion.contains(selectedDistrito, ignoreCase = true)
                    val precioNum = prop.precio.toDoubleOrNull() ?: 0.0
                    val minP = minPrecio.toDoubleOrNull() ?: 0.0
                    val maxP = maxPrecio.toDoubleOrNull() ?: Double.MAX_VALUE
                    val matchPrecio = precioNum in minP..maxP
                    val matchHab = prop.habitaciones >= minHabitaciones
                    val matchTags = selectedTags.isEmpty() || prop.etiquetasEntorno.containsAll(selectedTags)
                    var matchNear = true
                    if (filterNearMe && userLocation != null) { val results = FloatArray(1); Location.distanceBetween(userLocation!!.latitude, userLocation!!.longitude, prop.lat, prop.lng, results); matchNear = results[0] <= 5000 }
                    matchTipo && matchDistrito && matchPrecio && matchHab && matchTags && matchNear
                }

                if (isMapView) {
                    GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState, properties = mapProperties, uiSettings = mapUiSettings, onMapClick = { selectedProperty = null }) {
                        listaFiltrada.forEach { propiedad ->
                            Marker(
                                state = MarkerState(position = LatLng(propiedad.lat, propiedad.lng)),
                                title = propiedad.titulo,
                                snippet = "${propiedad.moneda} ${propiedad.precio}",
                                icon = LocationHelper.bitmapDescriptorFromVector(context, R.drawable.ic_custom_pin),
                                onClick = { selectedProperty = propiedad.copy(); false }
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 180.dp, bottom = 100.dp, start = 16.dp, end = 16.dp)) {
                        if (isLoadingData) { items(4) { SkeletonPropertyCard() } }
                        else if (listaFiltrada.isEmpty()) { item { Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.HomeWork, null, tint = Color.Gray, modifier = Modifier.size(60.dp)); Spacer(modifier = Modifier.height(8.dp)); Text("No hay propiedades aquí", color = Color.Gray) } } } }
                        else { items(listaFiltrada) { propiedad -> PropertyCard(propiedad = propiedad, isFavorite = propiedad.favoritos.contains(currentUser?.uid), onFavoriteClick = { toggleFavorite(propiedad) }, onClick = { selectedProperty = propiedad }) } }
                    }
                }

                // HEADER
                Column(modifier = Modifier.align(Alignment.TopCenter).background(Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background.copy(alpha = 0.9f), Color.Transparent))).padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            TextField(
                                value = searchQuery, onValueChange = { onSearch(it) }, placeholder = { Text("Buscar zona...", color = TextGray) },
                                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface),
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = Azul) }, singleLine = true
                            )
                            if (searchResults.isNotEmpty()) {
                                Card(modifier = Modifier.fillMaxWidth().padding(top = 56.dp).heightIn(max = 200.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(8.dp)) {
                                    LazyColumn { items(searchResults) { res -> DropdownMenuItem(text = { Text(res.featureName ?: res.thoroughfare ?: "", color = MaterialTheme.colorScheme.onSurface) }, onClick = { scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(res.latitude, res.longitude), 15f), 1000) }; searchQuery = ""; searchResults = emptyList(); isMapView = true }, leadingIcon = { Icon(Icons.Default.Place, null, tint = TextGray) }) } }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Azul).clickable { showFilterSheet = true }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Tune, null, tint = Color.White) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box {
                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).border(2.dp, Azul, CircleShape).clickable { showProfileMenu = true }) {
                                if (userImage.isNotEmpty()) Image(painter = rememberAsyncImagePainter(userImage), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape)) else Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(8.dp))
                            }
                            DropdownMenu(expanded = showProfileMenu, onDismissRequest = { showProfileMenu = false }, offset = DpOffset(x = (-10).dp, y = 8.dp), modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                DropdownMenuItem(text = { Text(if(isDarkTheme) "Modo Claro" else "Modo Oscuro") }, onClick = { isDarkTheme = !isDarkTheme; showProfileMenu = false }, leadingIcon = { Icon(if(isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, null) })
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text("Editar Perfil") }, onClick = { showProfileMenu = false; navigateToProfile() }, leadingIcon = { Icon(Icons.Default.Edit, null, tint = Azul) })
                                DropdownMenuItem(text = { Text("Cerrar Sesión", color = Color.Red) }, onClick = { showProfileMenu = false; showLogoutDialog = true }, leadingIcon = { Icon(Icons.Default.ExitToApp, null, tint = Color.Red) })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { SelectableChip(text = "Comprar", selected = filterSelected == "Comprar") { filterSelected = "Comprar" } }
                        item { SelectableChip(text = "Alquilar", selected = filterSelected == "Alquilar") { filterSelected = "Alquilar" } }
                        item { SelectableChip(text = "Cerca de mí", selected = filterNearMe, icon = Icons.Outlined.NearMe) { if (!hasLocationPermission) { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)) } else if (!checkGPSEnabled()) { requestEnableGPS() } else { fusedLocationClient.lastLocation.addOnSuccessListener { loc -> if (loc != null) userLocation = LatLng(loc.latitude, loc.longitude); filterNearMe = !filterNearMe } } } }
                    }
                }

                Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(onClick = { showPublishMenu = true }, modifier = Modifier.size(64.dp).glassEffect(backgroundColor = Azul, shape = CircleShape), shape = CircleShape, color = Color.Transparent) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(32.dp)) } }
                }
            }
        }
    }

    if (showFilterSheet) { ModalBottomSheet(onDismissRequest = { showFilterSheet = false }, containerColor = MaterialTheme.colorScheme.surface) { Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) { Text("Filtros", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Spacer(modifier = Modifier.height(16.dp)); Text("Estilo de Vida", fontWeight = FontWeight.Bold, color = Azul); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { availableTags.forEach { tag -> val isSelected = selectedTags.contains(tag); FilterChip(selected = isSelected, onClick = { selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag }, label = { Text(tag) }, leadingIcon = { Icon(imageVector = when(tag) { "Zona Segura"->Icons.Default.Security; "Cerca a Parque"->Icons.Default.Park; "Vista al Mar"->Icons.Default.Waves; else->Icons.Default.Store }, null, modifier=Modifier.size(16.dp)) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Azul, selectedLabelColor = Color.White)) } }; Spacer(modifier = Modifier.height(32.dp)); Button(onClick = { showFilterSheet = false }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Azul), shape = RoundedCornerShape(12.dp)) { Text("Ver Resultados", color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(modifier = Modifier.height(24.dp)) } } }
    if (showPublishMenu) { ModalBottomSheet(onDismissRequest = { showPublishMenu = false }, containerColor = MaterialTheme.colorScheme.surface) { Column(modifier = Modifier.padding(24.dp)) { Text("Publicar Propiedad", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Spacer(modifier = Modifier.height(24.dp)); Button(onClick = { showPublishMenu = false; navigateToPublish("Venta") }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Azul)) { Text("Vender", color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(modifier = Modifier.height(16.dp)); Button(onClick = { showPublishMenu = false; navigateToPublish("Alquiler") }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Azul.copy(alpha = 0.7f))) { Text("Alquilar", color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(modifier = Modifier.height(40.dp)) } } }

    // --- DETALLE DE PROPIEDAD ---
    selectedProperty?.let { initialProp ->
        val propiedad = propiedadesList.find { it.id == initialProp.id } ?: initialProp

        ModalBottomSheet(onDismissRequest = { selectedProperty = null }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {

                // HEADER CON UNA O VARIAS FOTOS
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    if (propiedad.imagenes.size == 1) {
                        val imgUrl = propiedad.imagenes.first()
                        AsyncImage(
                            model = imgUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).clickable { zoomImageSrc = imgUrl }
                        )
                    } else if (propiedad.imagenes.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(propiedad.imagenes) { imgUrl ->
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.width(340.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp)).clickable { zoomImageSrc = imgUrl }
                                )
                            }
                        }
                    }

                    // BOTÓN FAVORITO FLOTANTE (ARREGLADO Y ENCIMA DE TODO)
                    IconButton(
                        onClick = { toggleFavorite(propiedad) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(48.dp)
                            .glassEffect(backgroundColor = Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = if (propiedad.favoritos.contains(currentUser?.uid)) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (propiedad.favoritos.contains(currentUser?.uid)) Color(0xFFFF4081) else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // TITULO Y PRECIO
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(propiedad.titulo, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(propiedad.direccion, color = TextGray, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PRECIO DESTACADO
                Surface(color = Azul.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        "${propiedad.moneda} ${propiedad.precio}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Azul,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color.Gray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(24.dp))

                // CARACTERÍSTICAS
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PremiumFeatureItem(Icons.Default.Bed, "${propiedad.habitaciones} Hab")
                    PremiumFeatureItem(Icons.Default.Bathtub, "${propiedad.banos} Baños")
                    if(propiedad.area.isNotEmpty()) PremiumFeatureItem(Icons.Default.SquareFoot, "${propiedad.area} m²")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Descripción", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Text(propiedad.descripcion, color = TextGray, lineHeight = 24.sp, fontSize = 16.sp)

                Spacer(modifier = Modifier.height(32.dp))

                // BOTONES DE ACCIÓN
                Button(
                    onClick = { try { val url = "https://api.whatsapp.com/send?phone=51${propiedad.telefono}"; context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) }) } catch (e: Exception) {} },
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF25D366)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Message, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Contactar por WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { val gmmIntentUri = Uri.parse("geo:${propiedad.lat},${propiedad.lng}?q=${propiedad.lat},${propiedad.lng}(${Uri.encode(propiedad.titulo)})"); val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri); mapIntent.setPackage("com.google.android.apps.maps"); try { context.startActivity(mapIntent) } catch (e: Exception) {} },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Azul),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Azul)
                ) {
                    Icon(Icons.Default.Map, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver ubicación en Mapa", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// --- NUEVO HELPER PREMIUM PARA CARACTERÍSTICAS ---
@Composable
fun PremiumFeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(color = Azul.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Icon(icon, null, tint = Azul, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SelectableChip(text: String, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Surface(color = if (selected) Azul else MaterialTheme.colorScheme.surface, contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, shape = RoundedCornerShape(50), border = if (!selected) androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha=0.3f)) else null, modifier = Modifier.clickable { onClick() }) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { if (icon != null) { Icon(icon, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)) }; Text(text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) } }
}

@Composable
fun SkeletonPropertyCard() { Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).shadow(2.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column { Box(modifier = Modifier.height(200.dp).fillMaxWidth().shimmerEffect()); Column(modifier = Modifier.padding(16.dp)) { Box(modifier = Modifier.height(20.dp).fillMaxWidth(0.6f).shimmerEffect().clip(RoundedCornerShape(4.dp))); Spacer(modifier = Modifier.height(8.dp)); Box(modifier = Modifier.height(16.dp).fillMaxWidth(0.4f).shimmerEffect().clip(RoundedCornerShape(4.dp))) } } } }

// --- PROPERTY CARD PRINCIPAL (LISTA) ---
@Composable
fun PropertyCard(propiedad: Propiedad, isFavorite: Boolean, onFavoriteClick: () -> Unit, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp).clickable { onClick() }.shadow(6.dp, RoundedCornerShape(24.dp), spotColor = Color.Black), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            Box(modifier = Modifier.height(240.dp).fillMaxWidth()) {
                val image = propiedad.imagenes.firstOrNull() ?: propiedad.imagenes.toString()
                AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).glassEffect(backgroundColor = Azul.copy(alpha = 0.9f), shape = RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("${propiedad.moneda} ${propiedad.precio}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }

                // CORAZÓN
                IconButton(onClick = onFavoriteClick, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(44.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)) {
                    Icon(imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = null, tint = if(isFavorite) Color(0xFFFF4081) else Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.padding(20.dp)) {
                Text(propiedad.titulo, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, tint = TextGray, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(propiedad.direccion, color = TextGray, fontSize = 14.sp, maxLines = 1) }
                Spacer(modifier = Modifier.height(16.dp)); Divider(color = Color.Gray.copy(alpha = 0.2f)); Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (propiedad.habitaciones > 0) PremiumFeatureItem(Icons.Default.Bed, "${propiedad.habitaciones} Hab")
                    if (propiedad.banos > 0) PremiumFeatureItem(Icons.Default.Bathtub, "${propiedad.banos} Baños")
                    if (propiedad.area.isNotEmpty()) PremiumFeatureItem(Icons.Default.SquareFoot, "${propiedad.area} m²")
                }
            }
        }
    }
}

// Helpers Viejos (Mantenidos)
@Composable fun FeatureIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tooltip: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, contentDescription = tooltip, tint = Azul, modifier = Modifier.size(24.dp)); Text(tooltip, fontSize = 10.sp, color = TextGray) } }
@Composable fun FeatureRowWithPhoto(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, photoUrl: String, onPhotoClick: (String) -> Unit) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Azul, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface); Spacer(modifier = Modifier.weight(1f)); if (photoUrl.isNotEmpty()) { Text("Ver Foto", color = Azul, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onPhotoClick(photoUrl) }); Spacer(modifier = Modifier.width(8.dp)); AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).clickable { onPhotoClick(photoUrl) }, contentScale = ContentScale.Crop) } } }
@Composable fun ZoomableImageDialog(imageUrl: String, onDismiss: () -> Unit) { Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) { Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() }, contentAlignment = Alignment.Center) { var scale by remember { mutableStateOf(1f) }; var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }; AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y).pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> scale = maxOf(1f, minOf(3f, scale * zoom)); offset += pan } }, contentScale = ContentScale.Fit); IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) { Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White) } } } }