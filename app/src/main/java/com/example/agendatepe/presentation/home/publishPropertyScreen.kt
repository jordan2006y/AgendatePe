package com.example.agendatepe.presentation.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.agendatepe.ui.theme.*
import com.example.agendatepe.utils.LocationHelper
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishPropertyScreen(
    auth: FirebaseAuth,
    tipoOperacion: String,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- PROTECCIÓN CONTRA SALIDA ACCIDENTAL ---
    var showExitDialog by remember { mutableStateOf(false) }

    // Intercepta el botón "Atrás" del sistema
    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Salir sin publicar?") },
            text = { Text("Si sales ahora, perderás todos los datos ingresados.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Salir", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancelar", color = black) }
            },
            containerColor = Color.White
        )
    }
    // -------------------------------------------

    // --- CATEGORÍA DEL INMUEBLE ---
    val categories = listOf("Casa", "Departamento", "Oficina", "Terreno")
    var selectedCategory by rememberSaveable { mutableStateOf(categories[0]) }
    var expandedCategory by remember { mutableStateOf(false) }

    // Datos básicos
    var title by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("S/") }
    var description by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }

    // Contadores
    var bedrooms by rememberSaveable { mutableStateOf(1) }
    var bathrooms by rememberSaveable { mutableStateOf(1) }

    // Características
    var hasPool by rememberSaveable { mutableStateOf(false) }
    var hasGarage by rememberSaveable { mutableStateOf(false) }
    var hasGarden by rememberSaveable { mutableStateOf(false) }
    var isPetFriendly by rememberSaveable { mutableStateOf(false) }
    var hasPapers by rememberSaveable { mutableStateOf(false) }

    // Fotos extras
    var poolUri by remember { mutableStateOf<Uri?>(null) }
    var garageUri by remember { mutableStateOf<Uri?>(null) }
    var gardenUri by remember { mutableStateOf<Uri?>(null) }

    // Ubicación
    var address by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf(0.0) }
    var lng by rememberSaveable { mutableStateOf(0.0) }
    var locationStatus by rememberSaveable { mutableStateOf("Sin ubicación seleccionada") }

    var searchResults by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // LÓGICA DE PROTECCIÓN
    val isTerreno = selectedCategory == "Terreno"
    val isOficina = selectedCategory == "Oficina"

    LaunchedEffect(selectedCategory) {
        if (isTerreno) {
            bedrooms = 0
            bathrooms = 0
            hasPool = false
            hasGarage = false
            hasGarden = false
            isPetFriendly = false
            poolUri = null
            garageUri = null
            gardenUri = null
        } else if (isOficina) {
            hasPool = false
            poolUri = null
            if (bedrooms == 0) bedrooms = 1
            if (bathrooms == 0) bathrooms = 1
        } else {
            if (bedrooms == 0) bedrooms = 1
            if (bathrooms == 0) bathrooms = 1
        }
    }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val locationLat = savedStateHandle?.getLiveData<Double>("location_lat")?.observeAsState()
    val locationLng = savedStateHandle?.getLiveData<Double>("location_lng")?.observeAsState()
    val locationAddr = savedStateHandle?.getLiveData<String>("location_address")?.observeAsState()

    LaunchedEffect(locationLat?.value, locationLng?.value, locationAddr?.value) {
        if (locationLat?.value != null && locationLng?.value != null) {
            lat = locationLat!!.value!!
            lng = locationLng!!.value!!
            val addr = locationAddr?.value
            if (!addr.isNullOrEmpty() && addr != "Buscando...") {
                address = addr
                searchResults = emptyList()
            }
            locationStatus = "¡Ubicación exacta guardada!"
            savedStateHandle.remove<Double>("location_lat")
            savedStateHandle.remove<Double>("location_lng")
            savedStateHandle.remove<String>("location_address")
        }
    }

    fun onAddressChange(newAddress: String) {
        address = newAddress
        searchJob?.cancel()
        if (newAddress.isBlank()) { searchResults = emptyList(); return }
        searchJob = scope.launch { delay(800); searchResults = LocationHelper.searchPlaces(context, newAddress) }
    }

    val carouselPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickMultipleVisualMedia(5)) { uris -> selectedImages = uris }
    val poolPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri -> poolUri = uri }
    val garagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri -> garageUri = uri }
    val gardenPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri -> gardenUri = uri }

    fun saveProperty() {
        if (title.isEmpty() || price.isEmpty() || lat == 0.0 || selectedImages.isEmpty()) {
            Toast.makeText(context, "Faltan datos importantes (Título, Precio, Ubicación o Fotos)", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isTerreno) {
            if (bedrooms == 0 || bathrooms == 0) {
                Toast.makeText(context, "Una $selectedCategory debe tener al menos 1 cuarto/ambiente y 1 baño", Toast.LENGTH_LONG).show()
                return
            }
        }
        if (isOficina && hasPool) {
            Toast.makeText(context, "Una oficina no puede tener piscina", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        scope.launch {
            try {
                val userId = auth.currentUser?.uid ?: ""
                val userDoc = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
                val userPhone = userDoc.getString("phone") ?: ""

                val duplicates = FirebaseFirestore.getInstance().collection("propiedades")
                    .whereEqualTo("lat", lat).whereEqualTo("lng", lng).get().await()

                if (!duplicates.isEmpty) {
                    isLoading = false
                    Toast.makeText(context, "¡Error! Propiedad duplicada en esta ubicación.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val propId = UUID.randomUUID().toString()
                val storageRef = FirebaseStorage.getInstance().reference

                val uploadedCarouselUrls = mutableListOf<String>()
                selectedImages.forEachIndexed { index, uri ->
                    val ref = storageRef.child("propiedades/$propId/foto_$index.jpg")
                    ref.putFile(uri).await()
                    val url = ref.downloadUrl.await().toString()
                    uploadedCarouselUrls.add(url)
                }

                var poolUrl = ""
                if (hasPool && poolUri != null) {
                    val ref = storageRef.child("propiedades/$propId/feature_pool.jpg")
                    ref.putFile(poolUri!!).await()
                    poolUrl = ref.downloadUrl.await().toString()
                }

                var garageUrl = ""
                if (hasGarage && garageUri != null) {
                    val ref = storageRef.child("propiedades/$propId/feature_garage.jpg")
                    ref.putFile(garageUri!!).await()
                    garageUrl = ref.downloadUrl.await().toString()
                }

                var gardenUrl = ""
                if (hasGarden && gardenUri != null) {
                    val ref = storageRef.child("propiedades/$propId/feature_garden.jpg")
                    ref.putFile(gardenUri!!).await()
                    gardenUrl = ref.downloadUrl.await().toString()
                }

                val map = hashMapOf(
                    "id" to propId,
                    "userId" to userId,
                    "telefono" to userPhone,
                    "categoria" to selectedCategory,
                    "tipo" to tipoOperacion,
                    "titulo" to title,
                    "precio" to price,
                    "moneda" to currency,
                    "direccion" to address,
                    "descripcion" to description,
                    "area" to area,
                    "habitaciones" to bedrooms,
                    "banos" to bathrooms,
                    "tienePiscina" to hasPool,
                    "tieneCochera" to hasGarage,
                    "tieneJardin" to hasGarden,
                    "esPetFriendly" to isPetFriendly,
                    "papelesEnRegla" to hasPapers,
                    "fotoPiscina" to poolUrl,
                    "fotoCochera" to garageUrl,
                    "fotoJardin" to gardenUrl,
                    "lat" to lat,
                    "lng" to lng,
                    "imagenes" to uploadedCarouselUrls,
                    "imagen" to uploadedCarouselUrls.firstOrNull()
                )

                FirebaseFirestore.getInstance().collection("propiedades").document(propId).set(map).await()

                Toast.makeText(context, "¡Publicado con éxito!", Toast.LENGTH_LONG).show()
                navController.popBackStack()

            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(context, "Error al publicar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Azul).padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // BOTÓN ATRÁS PROTEGIDO
            IconButton(onClick = { showExitDialog = true }) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Text("Publicar $tipoOperacion", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("1. Fotos Principales (Máx 5)", color = Crema, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Box(modifier = Modifier.size(100.dp).background(Color.White, RoundedCornerShape(12.dp)).border(2.dp, Crema, RoundedCornerShape(12.dp)).clickable { carouselPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.AddPhotoAlternate, null, tint = Azul, modifier = Modifier.size(40.dp)) }
            }
            items(selectedImages) { uri -> AsyncImage(model = uri, contentDescription = null, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("2. Datos Principales", color = Crema, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expandedCategory,
            onExpandedChange = { expandedCategory = !expandedCategory },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo de Inmueble") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedTextColor = black, unfocusedTextColor = black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedCategory,
                onDismissRequest = { expandedCategory = false },
                modifier = Modifier.background(Color.White)
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category, color = black) },
                        onClick = {
                            selectedCategory = category
                            expandedCategory = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        CustomInput(value = title, onValueChange = { title = it }, label = "Título (Ej: Casa de Playa)", icon = Icons.Default.Home)
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = { currency = if (currency == "S/") "$" else "S/" }, colors = ButtonDefaults.buttonColors(containerColor = Crema), shape = RoundedCornerShape(12.dp), modifier = Modifier.height(56.dp).width(60.dp), contentPadding = PaddingValues(0.dp)) { Text(currency, color = black, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) { CustomInput(value = price, onValueChange = { price = it }, label = "Precio", icon = Icons.Default.AttachMoney, isNumber = true) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        CustomInput(value = area, onValueChange = { area = it }, label = "Área Total (m²)", icon = Icons.Default.SquareFoot, isNumber = true)

        Spacer(modifier = Modifier.height(24.dp))

        Text("3. Ubicación", color = Crema, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Box { CustomInput(value = address, onValueChange = { onAddressChange(it) }, label = "Dirección...", icon = Icons.Default.LocationOn) }
        if (searchResults.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(top = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                LazyColumn {
                    items(searchResults) { result ->
                        DropdownMenuItem(
                            text = { Column { Text(result.featureName ?: result.thoroughfare ?: "", color = black); Text("${result.subLocality ?: ""}, ${result.locality ?: ""}", fontSize = 12.sp, color = Color.Gray) } },
                            onClick = { address = "${result.featureName ?: result.thoroughfare}, ${result.subLocality ?: ""}"; lat = result.latitude; lng = result.longitude; locationStatus = "¡Ubicación guardada!"; searchResults = emptyList() },
                            leadingIcon = { Icon(Icons.Default.Place, null, tint = Color.Gray) }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { navController.navigate("pick_location") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = if (lat != 0.0) Color(0xFF4CAF50) else Azul), border = androidx.compose.foundation.BorderStroke(1.dp, Crema)) { Icon(Icons.Default.Map, null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text(text = if (lat != 0.0) locationStatus else "O buscar en el Mapa", color = Color.White, fontWeight = FontWeight.Bold) }

        if (!isTerreno) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("4. Características", color = Crema, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CounterControl(label = "Habitaciones", count = bedrooms, onIncrease = { bedrooms++ }, onDecrease = { if (bedrooms > 1) bedrooms-- })
                CounterControl(label = "Baños", count = bathrooms, onIncrease = { bathrooms++ }, onDecrease = { if (bathrooms > 1) bathrooms-- })
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (!isOficina) {
                FeatureWithPhotoControl(label = "Piscina", checked = hasPool, onCheckedChange = { hasPool = it }, imageUri = poolUri, onPickImage = { poolPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
            }
            FeatureWithPhotoControl(label = "Cochera", checked = hasGarage, onCheckedChange = { hasGarage = it }, imageUri = garageUri, onPickImage = { garagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
            FeatureWithPhotoControl(label = "Jardín", checked = hasGarden, onCheckedChange = { hasGarden = it }, imageUri = gardenUri, onPickImage = { gardenPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isPetFriendly, onCheckedChange = { isPetFriendly = it }, colors = CheckboxDefaults.colors(checkedColor = Crema, uncheckedColor = Color.White, checkmarkColor = black))
                Text("Pet Friendly", color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        CustomInput(value = description, onValueChange = { description = it }, label = "Descripción", icon = Icons.Default.Description, isMultiLine = true)

        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = hasPapers, onCheckedChange = { hasPapers = it }, colors = CheckboxDefaults.colors(checkedColor = Crema, uncheckedColor = Color.White, checkmarkColor = black)); Text("Documentos en regla.", color = Color.White, fontSize = 12.sp) }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Crema, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                onClick = { saveProperty() },
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(25.dp), colors = ButtonDefaults.buttonColors(containerColor = Crema)
            ) { Text("PUBLICAR PROPIEDAD", color = black, fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun FeatureWithPhotoControl(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, imageUri: Uri?, onPickImage: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = Crema, uncheckedColor = Color.White, checkmarkColor = black))
            Text(text = label, color = Color.White, fontSize = 14.sp)
            if (checked) {
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onPickImage, colors = ButtonDefaults.buttonColors(containerColor = if (imageUri != null) Color(0xFF4CAF50) else Color.White), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), modifier = Modifier.height(30.dp)) {
                    Icon(Icons.Default.CameraAlt, null, tint = if (imageUri != null) Color.White else Azul, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (imageUri != null) "Foto Lista" else "Añadir Foto", fontSize = 10.sp, color = if (imageUri != null) Color.White else Azul)
                }
            }
        }
        if (checked && imageUri != null) {
            AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.padding(start = 48.dp, bottom = 8.dp).size(60.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
fun CustomInput(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isNumber: Boolean = false, isMultiLine: Boolean = false) {
    TextField(value = value, onValueChange = onValueChange, label = { Text(label) }, leadingIcon = { Icon(icon, null, tint = Azul) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default, minLines = if (isMultiLine) 3 else 1, maxLines = if (isMultiLine) 5 else 1, colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedTextColor = black, unfocusedTextColor = black, focusedLabelColor = Azul, unfocusedLabelColor = Color.Gray, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
}

@Composable
fun CounterControl(label: String, count: Int, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(30.dp)) { Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = black) }
            Text("$count", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = black, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = onIncrease, modifier = Modifier.size(30.dp)) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = black) }
        }
    }
}