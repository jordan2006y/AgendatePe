package com.example.agendatepe.presentation.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.agendatepe.ui.theme.*
import com.example.agendatepe.utils.LocationHelper
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
    navController: NavController,
    viewModel: PublishPropertyViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // USAMOS EL PASO DEL VIEWMODEL (YA NO SE REINICIA)
    val totalSteps = 4

    var isLoading by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // --- VALIDACIÓN DE PASOS ---
    fun isStepValid(): Boolean {
        return when (viewModel.currentStep) {
            1 -> true
            2 -> viewModel.lat != 0.0 && viewModel.address.isNotEmpty()
            3 -> viewModel.title.isNotEmpty() && viewModel.price.isNotEmpty() && viewModel.area.isNotEmpty()
            4 -> viewModel.selectedImages.isNotEmpty()
            else -> false
        }
    }

    // --- PROTECCIÓN SALIDA ---
    BackHandler {
        if (viewModel.currentStep > 1) viewModel.currentStep-- else showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Salir?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Perderás el progreso de tu publicación.", color = Color.Gray) },
            confirmButton = { Button(onClick = { showExitDialog = false; navController.popBackStack() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Salir", color = Color.White) } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Continuar", color = MaterialTheme.colorScheme.onSurface) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // --- RECUPERAR DATOS DEL MAPA ---
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val locationLat = savedStateHandle?.getLiveData<Double>("location_lat")?.observeAsState()
    val locationLng = savedStateHandle?.getLiveData<Double>("location_lng")?.observeAsState()
    val locationAddr = savedStateHandle?.getLiveData<String>("location_address")?.observeAsState()

    LaunchedEffect(locationLat?.value, locationLng?.value) {
        if (locationLat?.value != null && locationLng?.value != null) {
            viewModel.updateLocation(locationLat!!.value!!, locationLng!!.value!!, locationAddr?.value ?: "")
            savedStateHandle.remove<Double>("location_lat")
            savedStateHandle.remove<Double>("location_lng")
            savedStateHandle.remove<String>("location_address")
        }
    }

    // --- FUNCIÓN GUARDAR ---
    fun saveProperty() {
        if (!isStepValid()) {
            Toast.makeText(context, "Completa los datos obligatorios", Toast.LENGTH_SHORT).show(); return
        }
        isLoading = true
        scope.launch {
            try {
                val userId = auth.currentUser?.uid ?: ""
                val userDoc = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
                val userPhone = userDoc.getString("phone") ?: ""

                val duplicates = FirebaseFirestore.getInstance().collection("propiedades")
                    .whereEqualTo("lat", viewModel.lat).whereEqualTo("lng", viewModel.lng).get().await()

                if (!duplicates.isEmpty) {
                    isLoading = false
                    Toast.makeText(context, "¡Ya existe una propiedad aquí!", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val propId = UUID.randomUUID().toString()
                val storageRef = FirebaseStorage.getInstance().reference

                val uploadedCarouselUrls = mutableListOf<String>()
                viewModel.selectedImages.forEachIndexed { index, uri ->
                    val ref = storageRef.child("propiedades/$propId/foto_$index.jpg")
                    ref.putFile(uri).await()
                    uploadedCarouselUrls.add(ref.downloadUrl.await().toString())
                }

                val poolUrl = if (viewModel.hasPool && viewModel.poolUri != null) storageRef.child("propiedades/$propId/pool.jpg").putFile(viewModel.poolUri!!).await().storage.downloadUrl.await().toString() else ""
                val garageUrl = if (viewModel.hasGarage && viewModel.garageUri != null) storageRef.child("propiedades/$propId/garage.jpg").putFile(viewModel.garageUri!!).await().storage.downloadUrl.await().toString() else ""
                val gardenUrl = if (viewModel.hasGarden && viewModel.gardenUri != null) storageRef.child("propiedades/$propId/garden.jpg").putFile(viewModel.gardenUri!!).await().storage.downloadUrl.await().toString() else ""

                val map = hashMapOf<String, Any>(
                    "id" to propId, "userId" to userId, "telefono" to userPhone,
                    "categoria" to viewModel.selectedCategory, "tipo" to tipoOperacion,
                    "titulo" to viewModel.title, "precio" to viewModel.price, "moneda" to viewModel.currency,
                    "direccion" to viewModel.address, "descripcion" to viewModel.description, "area" to viewModel.area,
                    "habitaciones" to viewModel.bedrooms, "banos" to viewModel.bathrooms,
                    "tienePiscina" to viewModel.hasPool, "tieneCochera" to viewModel.hasGarage,
                    "tieneJardin" to viewModel.hasGarden, "esPetFriendly" to viewModel.isPetFriendly,
                    "papelesEnRegla" to viewModel.hasPapers,
                    "fotoPiscina" to poolUrl, "fotoCochera" to garageUrl, "fotoJardin" to gardenUrl,
                    "lat" to viewModel.lat, "lng" to viewModel.lng,
                    "imagenes" to uploadedCarouselUrls,
                    "imagen" to (uploadedCarouselUrls.firstOrNull() ?: "")
                )

                FirebaseFirestore.getInstance().collection("propiedades").document(propId).set(map).await()
                Toast.makeText(context, "¡Publicado Exitosamente!", Toast.LENGTH_LONG).show()
                navController.popBackStack()
            } catch (e: Exception) { isLoading = false; Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }

    // --- UI ---
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Paso ${viewModel.currentStep} de $totalSteps", fontSize = 12.sp, color = Azul)
                            Text(
                                text = when(viewModel.currentStep){ 1->"Tipo de Inmueble"; 2->"Ubicación"; 3->"Detalles"; else->"Fotos & Publicar" },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (viewModel.currentStep > 1) viewModel.currentStep-- else showExitDialog = true }) {
                            Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                LinearProgressIndicator(
                    progress = { viewModel.currentStep.toFloat() / totalSteps.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Azul,
                    trackColor = Color.DarkGray
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (viewModel.currentStep > 1) {
                    OutlinedButton(
                        onClick = { viewModel.currentStep-- },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Gray),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("Atrás") }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                val isValid = isStepValid()

                Button(
                    onClick = {
                        if (isValid) {
                            if (viewModel.currentStep < totalSteps) viewModel.currentStep++ else saveProperty()
                        } else {
                            Toast.makeText(context, "Completa los datos para continuar", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isValid) Azul else Color.DarkGray,
                        contentColor = if (isValid) Color.White else Color.Gray
                    )
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text(if (viewModel.currentStep == totalSteps) "Publicar Ahora" else "Siguiente", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = viewModel.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "WizardAnimation"
            ) { step ->
                Column {
                    when (step) {
                        1 -> StepOneCategory(viewModel)
                        2 -> StepTwoLocation(viewModel, navController)
                        3 -> StepThreeDetails(viewModel)
                        4 -> StepFourPhotos(viewModel)
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// --- PASOS DEL WIZARD (Componentes Auxiliares) ---
@Composable fun StepOneCategory(viewModel: PublishPropertyViewModel) {
    Text("¿Qué tipo de propiedad es?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    Spacer(modifier = Modifier.height(24.dp))
    listOf("Casa", "Departamento", "Oficina", "Terreno").forEach { category ->
        val isSelected = viewModel.selectedCategory == category
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { viewModel.onCategoryChange(category) }, colors = CardDefaults.cardColors(containerColor = if (isSelected) Azul.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface), border = if (isSelected) BorderStroke(2.dp, Azul) else null, shape = RoundedCornerShape(16.dp)) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = when(category){"Casa"->Icons.Default.Home; "Departamento"->Icons.Default.Apartment; "Oficina"->Icons.Default.Business; else->Icons.Default.Landscape}, contentDescription = null, tint = if(isSelected) Azul else Color.Gray, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(category, fontSize = 18.sp, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.weight(1f))
                if(isSelected) Icon(Icons.Default.CheckCircle, null, tint = Azul)
            }
        }
    }
}

@Composable fun StepTwoLocation(viewModel: PublishPropertyViewModel, navController: NavController) {
    Text("¿Dónde se ubica?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    Spacer(modifier = Modifier.height(24.dp))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, tint = Azul); Spacer(modifier = Modifier.width(8.dp)); Text("Dirección Referencial", color = Azul, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = if(viewModel.address.isEmpty()) "Selecciona en el mapa..." else viewModel.address, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = { navController.navigate("pick_location") }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Azul)) { Icon(Icons.Default.Map, null, tint = Azul); Spacer(modifier = Modifier.width(12.dp)); Text(if(viewModel.lat != 0.0) "Cambiar Ubicación" else "Abrir Mapa", color = Azul, fontWeight = FontWeight.Bold) }
    if (viewModel.lat != 0.0) { Spacer(modifier = Modifier.height(16.dp)); Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Ubicación confirmada", color = Color(0xFF4CAF50), fontSize = 14.sp) } }
}

@Composable fun StepThreeDetails(viewModel: PublishPropertyViewModel) {
    Text("Cuéntanos más", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    Spacer(modifier = Modifier.height(24.dp))
    CustomInput(value = viewModel.title, onValueChange = { viewModel.title = it }, label = "Título del Anuncio", icon = Icons.Default.Title)
    Spacer(modifier = Modifier.height(16.dp))
    Row { Surface(onClick = { viewModel.currency = if (viewModel.currency == "S/") "$" else "S/" }, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, Color.Gray), modifier = Modifier.height(56.dp).width(70.dp)) { Box(contentAlignment = Alignment.Center) { Text(viewModel.currency, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp) } }; Spacer(modifier = Modifier.width(8.dp)); Box(modifier = Modifier.weight(1f)) { CustomInput(value = viewModel.price, onValueChange = { viewModel.price = it }, label = "Precio", icon = Icons.Default.AttachMoney, isNumber = true) } }
    Spacer(modifier = Modifier.height(16.dp))
    CustomInput(value = viewModel.area, onValueChange = { viewModel.area = it }, label = "Área (m²)", icon = Icons.Default.SquareFoot, isNumber = true)
    if (viewModel.selectedCategory != "Terreno") {
        Spacer(modifier = Modifier.height(24.dp)); Text("Distribución", color = Azul, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { CounterControl("Habitaciones", viewModel.bedrooms, { viewModel.bedrooms++ }, { if (viewModel.bedrooms > 1) viewModel.bedrooms-- }); CounterControl("Baños", viewModel.bathrooms, { viewModel.bathrooms++ }, { if (viewModel.bathrooms > 1) viewModel.bathrooms-- }) }
    }
    Spacer(modifier = Modifier.height(24.dp)); CustomInput(value = viewModel.description, onValueChange = { viewModel.description = it }, label = "Descripción", icon = Icons.Default.Description, isMultiLine = true)
}

@Composable fun StepFourPhotos(viewModel: PublishPropertyViewModel) {
    val carouselPicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickMultipleVisualMedia(5)) { if(it.isNotEmpty()) viewModel.selectedImages = it }
    Text("Galería Visual", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground); Text("Añade fotos de alta calidad.", color = Color.Gray, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(24.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
        item { Card(modifier = Modifier.size(140.dp).clickable { carouselPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Azul.copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp)) { Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Rounded.AddPhotoAlternate, null, tint = Azul, modifier = Modifier.size(40.dp)); Spacer(modifier = Modifier.height(8.dp)); Text("Añadir Fotos", color = Azul, fontWeight = FontWeight.Medium) } } }
        items(viewModel.selectedImages) { uri -> Card(modifier = Modifier.size(140.dp), shape = RoundedCornerShape(16.dp)) { Box(modifier = Modifier.fillMaxSize()) { AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop); IconButton(onClick = { viewModel.selectedImages = viewModel.selectedImages.filter { it != uri } }, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)) { Icon(Icons.Rounded.Close, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(16.dp)) } } } }
    }
    Spacer(modifier = Modifier.height(32.dp))
    Text("Adicionales", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground); Spacer(modifier = Modifier.height(12.dp))
    Column { FeatureCheckItem("Piscina", viewModel.hasPool) { viewModel.hasPool = it }; FeatureCheckItem("Cochera", viewModel.hasGarage) { viewModel.hasGarage = it }; FeatureCheckItem("Jardín", viewModel.hasGarden) { viewModel.hasGarden = it }; FeatureCheckItem("Pet Friendly 🐾", viewModel.isPetFriendly) { viewModel.isPetFriendly = it }; FeatureCheckItem("Papeles en Regla 📄", viewModel.hasPapers) { viewModel.hasPapers = it } }
}

@Composable fun CustomInput(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isNumber: Boolean = false, isMultiLine: Boolean = false) { OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, leadingIcon = { Icon(icon, null, tint = Azul) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default, minLines = if (isMultiLine) 3 else 1, maxLines = if (isMultiLine) 5 else 1, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Azul, unfocusedBorderColor = Color.Gray, focusedLabelColor = Azul, unfocusedLabelColor = Color.Gray, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface)) }
@Composable fun CounterControl(label: String, count: Int, onIncrease: () -> Unit, onDecrease: () -> Unit) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = Color.Gray, fontSize = 12.sp); Spacer(modifier = Modifier.height(8.dp)); Row(modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50)).border(1.dp, Color.Gray, RoundedCornerShape(50)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onDecrease, modifier = Modifier.size(36.dp)) { Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }; Text("$count", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 12.dp)); IconButton(onClick = onIncrease, modifier = Modifier.size(36.dp)) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Azul) } } } }
@Composable fun FeatureCheckItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(if (checked) Azul.copy(alpha = 0.15f) else Color.Transparent).clickable { onCheckedChange(!checked) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = if(checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if(checked) Azul else Color.Gray); Spacer(modifier = Modifier.width(12.dp)); Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = if(checked) FontWeight.Bold else FontWeight.Normal) } }