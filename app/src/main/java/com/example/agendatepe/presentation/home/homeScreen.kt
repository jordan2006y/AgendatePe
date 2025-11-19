package com.example.agendatepe.presentation.home

// ... (Imports iguales, asegúrate de importar LocationHelper)
import com.example.agendatepe.utils.LocationHelper // <--- IMPORTANTE
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.agendatepe.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

// (Data class Propiedad igual que antes...)
data class Propiedad(
    val id: String = "",
    val userId: String = "",
    val titulo: String = "",
    val precio: String = "",
    val moneda: String = "S/",
    val direccion: String = "",
    val descripcion: String = "",
    val area: String = "",
    val habitaciones: Int = 0,
    val banos: Int = 0,
    val imagenes: List<String> = emptyList(),
    val tipo: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val tienePiscina: Boolean = false,
    val tieneCochera: Boolean = false,
    val tieneJardin: Boolean = false,
    val esPetFriendly: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    navigateToPublish: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val lima = LatLng(-12.0464, -77.0428)
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(lima, 12f) }

    // Estados Mapa y Permisos
    var hasLocationPermission by remember { mutableStateOf(false) }
    var mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = false)) }
    var mapUiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)) }

    // Buscador
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Función Buscar con Helper
    fun onSearch(query: String) {
        searchQuery = query
        searchJob?.cancel()
        if (query.isBlank()) { searchResults = emptyList(); return }
        searchJob = scope.launch {
            delay(800)
            searchResults = LocationHelper.searchPlaces(context, query)
        }
    }

    // ... (Resto de variables: filterSelected, etc. IGUAL) ...
    var filterSelected by remember { mutableStateOf("Comprar") }
    var showProfileMenu by remember { mutableStateOf(false) }
    var showPublishMenu by remember { mutableStateOf(false) }
    var selectedProperty by remember { mutableStateOf<Propiedad?>(null) }
    var propiedadesList by remember { mutableStateOf<List<Propiedad>>(emptyList()) }

    // ... (LaunchedEffect de Firebase y Permisos IGUAL) ...
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("propiedades").addSnapshotListener { value, _ ->
            if (value != null) {
                propiedadesList = value.documents.mapNotNull { doc ->
                    try {
                        val rawList = doc.get("imagenes") as? List<*>
                        val listaSegura = rawList?.map { it.toString() } ?: emptyList()
                        val imgs = if (listaSegura.isNotEmpty()) listaSegura else listOf(doc.getString("imagen") ?: "")
                        Propiedad(
                            id = doc.id, userId = doc.getString("userId") ?: "", titulo = doc.getString("titulo") ?: "",
                            precio = doc.getString("precio") ?: "", moneda = doc.getString("moneda") ?: "S/",
                            direccion = doc.getString("direccion") ?: "", descripcion = doc.getString("descripcion") ?: "",
                            area = doc.getString("area") ?: "", habitaciones = (doc.getLong("habitaciones") ?: 0).toInt(),
                            banos = (doc.getLong("banos") ?: 0).toInt(), imagenes = imgs,
                            tipo = doc.getString("tipo") ?: "Venta", lat = doc.getDouble("lat") ?: 0.0, lng = doc.getDouble("lng") ?: 0.0,
                            tienePiscina = doc.getBoolean("tienePiscina") ?: false, tieneCochera = doc.getBoolean("tieneCochera") ?: false,
                            tieneJardin = doc.getBoolean("tieneJardin") ?: false, esPetFriendly = doc.getBoolean("esPetFriendly") ?: false
                        )
                    } catch (e: Exception) { null }
                }
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            mapProperties = mapProperties.copy(isMyLocationEnabled = true); mapUiSettings = mapUiSettings.copy(myLocationButtonEnabled = true)
            try { fusedLocationClient.lastLocation.addOnSuccessListener { location -> location?.let { cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f)) } } } catch (e: SecurityException) { }
        }
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true; mapProperties = mapProperties.copy(isMyLocationEnabled = true); mapUiSettings = mapUiSettings.copy(myLocationButtonEnabled = true)
        } else { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    }
    var userImage by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { val uid = Firebase.auth.currentUser?.uid; if (uid != null) FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener { document -> userImage = document.getString("image") ?: "" } }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState, properties = mapProperties, uiSettings = mapUiSettings, onMapClick = { selectedProperty = null }) {
            val filtro = if (filterSelected == "Comprar") "Venta" else "Alquiler"
            propiedadesList.filter { it.tipo == filtro }.forEach { propiedad ->
                Marker(state = MarkerState(position = LatLng(propiedad.lat, propiedad.lng)), title = propiedad.titulo, snippet = "${propiedad.moneda} ${propiedad.precio}", onClick = { selectedProperty = propiedad.copy(); false })
            }
        }

        Column(modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp, start = 16.dp, end = 16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // --- BUSCADOR HOME CON AUTOCOMPLETADO ---
                Box(modifier = Modifier.weight(1f)) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { onSearch(it) },
                        placeholder = { Text("Buscar zona...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(25.dp)),
                        shape = RoundedCornerShape(25.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { onSearch("") }) { Icon(Icons.Default.Close, null, tint = Color.Gray) } else Icon(Icons.Default.Search, null, tint = Azul) },
                        singleLine = true
                    )
                    // Lista de resultados Home
                    if (searchResults.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth().padding(top = 56.dp).heightIn(max = 200.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                            LazyColumn {
                                items(searchResults) { res ->
                                    DropdownMenuItem(
                                        text = { Text(res.featureName ?: res.thoroughfare ?: "", color = black) },
                                        onClick = {
                                            scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(res.latitude, res.longitude), 15f), 1000) }
                                            searchQuery = ""; searchResults = emptyList()
                                        },
                                        leadingIcon = { Icon(Icons.Default.Place, null, tint = Color.Gray) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))
                Box {
                    Box(modifier = Modifier.size(50.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(Crema).border(2.dp, Color.White, CircleShape).clickable { showProfileMenu = true }, contentAlignment = Alignment.Center) {
                        if (userImage.isNotEmpty()) Image(painter = rememberAsyncImagePainter(userImage), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Icon(Icons.Default.Person, contentDescription = null, tint = black)
                    }
                    DropdownMenu(expanded = showProfileMenu, onDismissRequest = { showProfileMenu = false }, offset = DpOffset(x = 0.dp, y = 10.dp), modifier = Modifier.background(Color.White)) {
                        DropdownMenuItem(text = { Text("Editar Perfil", color = black) }, onClick = { showProfileMenu = false; navigateToProfile() }, leadingIcon = { Icon(Icons.Default.Edit, null, tint = Azul) })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Cerrar Sesión", color = Color.Red) }, onClick = { showProfileMenu = false; onLogout() }, leadingIcon = { Icon(Icons.Default.ExitToApp, null, tint = Color.Red) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Filtros (Igual que antes)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                val isComprar = filterSelected == "Comprar"
                FilterChip(selected = isComprar, onClick = { filterSelected = "Comprar" }, label = { Text("Comprar", color = if (isComprar) Color.Black else Color.Gray, fontWeight = FontWeight.Bold) }, leadingIcon = { Icon(Icons.Default.Home, null, tint = if (isComprar) Color.Black else Color.Gray) }, shape = RoundedCornerShape(50), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Crema, containerColor = Color.White, selectedLabelColor = Color.Black), border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isComprar, borderColor = Color.LightGray, selectedBorderColor = Color.Black, borderWidth = 1.dp), modifier = Modifier.height(40.dp))
                Spacer(modifier = Modifier.width(12.dp))
                val isAlquilar = filterSelected == "Alquilar"
                FilterChip(selected = isAlquilar, onClick = { filterSelected = "Alquilar" }, label = { Text("Alquilar", color = if (isAlquilar) Color.Black else Color.Gray, fontWeight = FontWeight.Bold) }, leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = if (isAlquilar) Color.Black else Color.Gray) }, shape = RoundedCornerShape(50), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Crema, containerColor = Color.White, selectedLabelColor = Color.Black), border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isAlquilar, borderColor = Color.LightGray, selectedBorderColor = Color.Black, borderWidth = 1.dp), modifier = Modifier.height(40.dp))
            }
        }

        // Botones Publicar y Detalle (Igual que antes)
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            FloatingActionButton(onClick = { showPublishMenu = true }, containerColor = Crema, contentColor = black, modifier = Modifier.size(70.dp), shape = CircleShape) { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp)) }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(color = Crema, shape = RoundedCornerShape(12.dp), modifier = Modifier.shadow(4.dp, RoundedCornerShape(12.dp))) { Text("Publicar", color = black, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }
        }
        if (showPublishMenu) {
            ModalBottomSheet(onDismissRequest = { showPublishMenu = false }, containerColor = Color.White) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("¿Qué deseas hacer?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = black)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showPublishMenu = false; navigateToPublish("Venta") }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Azul)) { Text("Vender una Propiedad", color = Color.White, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showPublishMenu = false; navigateToPublish("Alquiler") }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Crema)) { Text("Poner en Alquiler", color = black, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
        selectedProperty?.let { propiedad ->
            ModalBottomSheet(onDismissRequest = { selectedProperty = null }, containerColor = Color.White) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (propiedad.imagenes.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(propiedad.imagenes) { imgUrl ->
                                AsyncImage(model = imgUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.width(280.dp).height(200.dp).clip(RoundedCornerShape(12.dp)))
                            }
                        }
                    } else { Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.LightGray, RoundedCornerShape(12.dp))) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(propiedad.titulo, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = black)
                    Text("${propiedad.moneda} ${propiedad.precio}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Azul)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(propiedad.direccion, color = Color.Gray, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if(propiedad.area.isNotEmpty()) FeatureIcon(Icons.Default.SquareFoot, "${propiedad.area} m²")
                        FeatureIcon(Icons.Default.Bed, "${propiedad.habitaciones} Hab")
                        FeatureIcon(Icons.Default.Bathtub, "${propiedad.banos} Baños")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (propiedad.tienePiscina) FeatureIcon(Icons.Default.Pool, "Piscina")
                        if (propiedad.tieneCochera) FeatureIcon(Icons.Default.Garage, "Cochera")
                        if (propiedad.tieneJardin) FeatureIcon(Icons.Default.Grass, "Jardín")
                        if (propiedad.esPetFriendly) FeatureIcon(Icons.Default.Pets, "Mascotas")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(propiedad.descripcion, color = black, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = {
                        val gmmIntentUri = Uri.parse("geo:${propiedad.lat},${propiedad.lng}?q=${propiedad.lat},${propiedad.lng}(${Uri.encode(propiedad.titulo)})")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try { context.startActivity(mapIntent) } catch (e: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://googleusercontent.com/maps.google.com"))) }
                    }, modifier = Modifier.fillMaxWidth(), border = androidx.compose.foundation.BorderStroke(1.dp, Azul)
                    ) { Icon(Icons.Default.Map, null, tint = Azul); Spacer(modifier = Modifier.width(8.dp)); Text("Ver ubicación en Google Maps", color = Azul) }
                    Spacer(modifier = Modifier.height(12.dp))
                    val currentUserId = Firebase.auth.currentUser?.uid
                    if (currentUserId != null && currentUserId == propiedad.userId) {
                        Button(onClick = { FirebaseFirestore.getInstance().collection("propiedades").document(propiedad.id).delete().addOnSuccessListener { selectedProperty = null; Toast.makeText(context, "Propiedad eliminada", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Icon(Icons.Default.Delete, null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Eliminar mi Publicación", color = Color.White, fontWeight = FontWeight.Bold) }
                    } else {
                        Button(onClick = {
                            try {
                                val phone = "51910202020"
                                val message = "Hola, estoy interesado en ${propiedad.titulo}."
                                val url = "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}"
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) })
                            } catch (e: Exception) { Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show() }
                        }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) { Icon(Icons.Default.Message, null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Contactar por WhatsApp", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun FeatureIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tooltip: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = tooltip, tint = Azul, modifier = Modifier.size(28.dp))
        Text(tooltip, fontSize = 10.sp, color = Color.Gray)
    }
}