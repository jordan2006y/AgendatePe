package com.example.agendatepe.presentation.home

// ... (Imports iguales) ...
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import java.util.UUID

@Composable
fun PublishPropertyScreen(
    auth: FirebaseAuth,
    tipoOperacion: String,
    navController: NavController
) {
    // ... (El cuerpo de la función es IDÉNTICO al anterior, no cambia nada arriba) ...
    // ... (Solo copia el cuerpo que te di en la respuesta anterior para esta parte) ...
    // ... (Lo importante es cambiar la función CustomInput al final) ...

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("S/") }
    var description by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var bedrooms by remember { mutableStateOf(1) }
    var bathrooms by remember { mutableStateOf(1) }
    var hasPool by remember { mutableStateOf(false) }
    var hasGarage by remember { mutableStateOf(false) }
    var hasGarden by remember { mutableStateOf(false) }
    var isPetFriendly by remember { mutableStateOf(false) }
    var hasPapers by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf(0.0) }
    var lng by remember { mutableStateOf(0.0) }
    var locationStatus by remember { mutableStateOf("Sin ubicación seleccionada") }
    var searchResults by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val locationLat = savedStateHandle?.getLiveData<Double>("location_lat")?.observeAsState()
    val locationLng = savedStateHandle?.getLiveData<Double>("location_lng")?.observeAsState()
    val locationAddr = savedStateHandle?.getLiveData<String>("location_address")?.observeAsState()

    LaunchedEffect(locationLat?.value, locationLng?.value, locationAddr?.value) {
        if (locationLat?.value != null && locationLng?.value != null) {
            lat = locationLat!!.value!!
            lng = locationLng!!.value!!
            val addr = locationAddr?.value
            if (!addr.isNullOrEmpty() && addr != "Buscando...") { address = addr; searchResults = emptyList() }
            locationStatus = "¡Ubicación exacta guardada!"
            savedStateHandle.remove<Double>("location_lat"); savedStateHandle.remove<Double>("location_lng"); savedStateHandle.remove<String>("location_address")
        }
    }
    fun onAddressChange(newAddress: String) {
        address = newAddress
        searchJob?.cancel()
        if (newAddress.isBlank()) { searchResults = emptyList(); return }
        searchJob = scope.launch { delay(800); searchResults = LocationHelper.searchPlaces(context, newAddress) }
    }
    val multiplePhotoPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickMultipleVisualMedia(5)) { uris -> selectedImages = uris }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    Column(modifier = Modifier.fillMaxSize().background(Azul).padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text("Publicar $tipoOperacion", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("1. Fotos (Máx 5)", color = Crema, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Box(modifier = Modifier.size(100.dp).background(Color.White, RoundedCornerShape(12.dp)).border(2.dp, Crema, RoundedCornerShape(12.dp)).clickable { multiplePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.AddPhotoAlternate, null, tint = Azul, modifier = Modifier.size(40.dp)) }
            }
            items(selectedImages) { uri -> AsyncImage(model = uri, contentDescription = null, modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop) }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("2. Datos Principales", color = Crema, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        CustomInput(value = title, onValueChange = { title = it }, label = "Título", icon = Icons.Default.Home)
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
        Spacer(modifier = Modifier.height(24.dp))
        Text("4. Características", color = Crema, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CounterControl(label = "Habitaciones", count = bedrooms, onIncrease = { bedrooms++ }, onDecrease = { if (bedrooms > 0) bedrooms-- })
            CounterControl(label = "Baños", count = bathrooms, onIncrease = { bathrooms++ }, onDecrease = { if (bathrooms > 0) bathrooms-- })
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) { FeatureCheckbox("Piscina", hasPool) { hasPool = it }; FeatureCheckbox("Cochera", hasGarage) { hasGarage = it } }
            Column(modifier = Modifier.weight(1f)) { FeatureCheckbox("Jardín", hasGarden) { hasGarden = it }; FeatureCheckbox("Pet Friendly", isPetFriendly) { isPetFriendly = it } }
        }
        Spacer(modifier = Modifier.height(16.dp))
        CustomInput(value = description, onValueChange = { description = it }, label = "Descripción", icon = Icons.Default.Description, isMultiLine = true)
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = hasPapers, onCheckedChange = { hasPapers = it }, colors = CheckboxDefaults.colors(checkedColor = Crema, uncheckedColor = Color.White, checkmarkColor = black)); Text("Documentos en regla.", color = Color.White, fontSize = 12.sp) }
        Spacer(modifier = Modifier.height(32.dp))
        if (isLoading) { CircularProgressIndicator(color = Crema, modifier = Modifier.align(Alignment.CenterHorizontally)) } else {
            Button(
                onClick = {
                    if (title.isNotEmpty() && price.isNotEmpty() && lat != 0.0 && selectedImages.isNotEmpty()) {
                        isLoading = true
                        FirebaseFirestore.getInstance().collection("propiedades").whereEqualTo("lat", lat).whereEqualTo("lng", lng).get().addOnSuccessListener { documents ->
                            if (!documents.isEmpty) { isLoading = false; Toast.makeText(context, "¡Error! Propiedad duplicada.", Toast.LENGTH_LONG).show() } else {
                                val propId = UUID.randomUUID().toString(); val uploadedUrls = mutableListOf<String>()
                                fun uploadImageRecursive(index: Int) {
                                    if (index >= selectedImages.size) {
                                        val map = hashMapOf("id" to propId, "userId" to auth.currentUser?.uid, "tipo" to tipoOperacion, "titulo" to title, "precio" to price, "moneda" to currency, "direccion" to address, "descripcion" to description, "area" to area, "habitaciones" to bedrooms, "banos" to bathrooms, "tienePiscina" to hasPool, "tieneCochera" to hasGarage, "tieneJardin" to hasGarden, "esPetFriendly" to isPetFriendly, "papelesEnRegla" to hasPapers, "lat" to lat, "lng" to lng, "imagenes" to uploadedUrls, "imagen" to uploadedUrls.firstOrNull())
                                        FirebaseFirestore.getInstance().collection("propiedades").document(propId).set(map).addOnSuccessListener { Toast.makeText(context, "¡Publicado!", Toast.LENGTH_LONG).show(); navController.popBackStack() }
                                        return
                                    }
                                    val ref = FirebaseStorage.getInstance().reference.child("propiedades/$propId/foto_$index.jpg")
                                    ref.putFile(selectedImages[index]).addOnSuccessListener { ref.downloadUrl.addOnSuccessListener { url -> uploadedUrls.add(url.toString()); uploadImageRecursive(index + 1) } }
                                }
                                uploadImageRecursive(0)
                            }
                        }
                    } else { Toast.makeText(context, "Faltan datos.", Toast.LENGTH_SHORT).show() }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(25.dp), colors = ButtonDefaults.buttonColors(containerColor = Crema)
            ) { Text("PUBLICAR PROPIEDAD", color = black, fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}


@Composable
fun CustomInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isNumber: Boolean = false,
    isMultiLine: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = Azul) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        minLines = if (isMultiLine) 3 else 1,
        maxLines = if (isMultiLine) 5 else 1,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedTextColor = black,
            unfocusedTextColor = black,
            focusedLabelColor = Azul,
            unfocusedLabelColor = Color.Gray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun CounterControl(label: String, count: Int, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(30.dp)) {
                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = black)
            }
            Text(
                "$count",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = black,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = onIncrease, modifier = Modifier.size(30.dp)) {
                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = black)
            }
        }
    }
}

@Composable
fun FeatureCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Crema,
                uncheckedColor = Color.White,
                checkmarkColor = black
            )
        )
        Text(text = label, color = Color.White, fontSize = 14.sp)
    }
}