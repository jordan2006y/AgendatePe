package com.example.agendatepe.presentation.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.agendatepe.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

@Composable
fun ProfileScreen(
    auth: FirebaseAuth,
    emailRecibido: String,
    passwordRecibido: String,
    navigateToHome: () -> Unit,
    onCancel: () -> Unit // Parámetro para cancelar registro
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Validación visual (si hay texto y no cumple los 9 dígitos)
    val isPhoneError = phone.isNotEmpty() && phone.length < 9

    // --- PROTECCIÓN DE SALIDA ---
    var showExitDialog by remember { mutableStateOf(false) }
    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Cancelar registro?", color = MainWhite) },
            text = { Text("Si sales ahora, se cerrará la sesión y no se guardarán tus datos.", color = TextGray) },
            confirmButton = {
                Button(
                    onClick = { showExitDialog = false; onCancel() }, // Llama a la limpieza
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Salir", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continuar", color = MainWhite)
                }
            },
            containerColor = DarkSurface
        )
    }

    val googleUser = auth.currentUser
    val isGoogleLogin = passwordRecibido == "GOOGLE_LOGIN" || (googleUser != null && passwordRecibido.isEmpty())

    LaunchedEffect(Unit) {
        if (isGoogleLogin && googleUser != null) {
            val rawName = googleUser.displayName ?: ""
            // Limpiamos el nombre de Google por si trae números
            name = rawName.filter { !it.isDigit() }
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> imageUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "AgendatePé", color = Azul, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Configura tu Perfil",
            color = MainWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Completa tus datos para finalizar.",
            color = TextGray,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- FOTO ---
        Box(
            modifier = Modifier
                .size(130.dp)
                .clickable { pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(2.dp, Azul, CircleShape)
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(60.dp))
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Azul)
                    .border(2.dp, DarkBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Subir foto", color = Azul, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.height(40.dp))

        // --- INPUT: NOMBRE (Sin Números) ---
        OutlinedTextField(
            value = name,
            onValueChange = { input ->
                // Valida que NO tenga dígitos
                if (input.none { it.isDigit() }) {
                    name = input
                }
            },
            label = { Text("Nombre Completo") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Person, null, tint = TextGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Azul,
                unfocusedBorderColor = Color.DarkGray,
                focusedLabelColor = Azul,
                unfocusedLabelColor = TextGray,
                focusedTextColor = MainWhite,
                unfocusedTextColor = MainWhite,
                cursorColor = Azul,
                focusedContainerColor = DarkBackground,
                unfocusedContainerColor = DarkBackground
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- INPUT: TELÉFONO (Empieza con 9, solo 9 dígitos) ---
        OutlinedTextField(
            value = phone,
            onValueChange = { input ->
                if (input.all { it.isDigit() } && input.length <= 9) {
                    // Solo permite escribir si empieza con 9 o está vacío
                    if (input.isEmpty() || input.startsWith("9")) {
                        phone = input
                    }
                }
            },
            label = { Text("Celular") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = TextGray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = isPhoneError,
            supportingText = {
                if (isPhoneError) {
                    Text(text = "Debe tener 9 dígitos", color = Color.Red)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isPhoneError) Color.Red else Azul,
                unfocusedBorderColor = if (isPhoneError) Color.Red else Color.DarkGray,
                focusedLabelColor = if (isPhoneError) Color.Red else Azul,
                unfocusedLabelColor = TextGray,
                focusedTextColor = MainWhite,
                unfocusedTextColor = MainWhite,
                cursorColor = Azul,
                focusedContainerColor = DarkBackground,
                unfocusedContainerColor = DarkBackground,
                errorLabelColor = Color.Red,
                errorBorderColor = Color.Red,
                errorLeadingIconColor = Color.Red
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Azul)
        } else {
            Button(
                onClick = {
                    val isValidName = name.isNotBlank() && name.length >= 3
                    val isValidPhone = phone.length == 9 && phone.startsWith("9")

                    if (isValidName && isValidPhone) {
                        isLoading = true
                        fun saveFirestoreData(uid: String) {
                            val saveData = { url: String ->
                                val map = hashMapOf(
                                    "id" to uid,
                                    "name" to name.trim(),
                                    "phone" to phone,
                                    "email" to (auth.currentUser?.email ?: emailRecibido),
                                    "image" to url
                                )
                                FirebaseFirestore.getInstance().collection("users").document(uid)
                                    .set(map, SetOptions.merge())
                                    .addOnSuccessListener {
                                        isLoading = false
                                        navigateToHome()
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            if (imageUri != null) {
                                val ref = FirebaseStorage.getInstance().reference.child("profile_images/$uid.jpg")
                                ref.putFile(imageUri!!).addOnSuccessListener {
                                    ref.downloadUrl.addOnSuccessListener { url -> saveData(url.toString()) }
                                }
                            } else {
                                val currentPhoto = auth.currentUser?.photoUrl?.toString() ?: ""
                                saveData(currentPhoto)
                            }
                        }

                        if (isGoogleLogin) {
                            val uid = auth.currentUser?.uid
                            if (uid != null) saveFirestoreData(uid)
                        } else {
                            auth.createUserWithEmailAndPassword(emailRecibido, passwordRecibido)
                                .addOnSuccessListener { res ->
                                    val uid = res.user!!.uid
                                    saveFirestoreData(uid)
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        if (!isValidName) Toast.makeText(context, "Ingresa un nombre válido", Toast.LENGTH_SHORT).show()
                        if (!isValidPhone) Toast.makeText(context, "El celular debe empezar con 9 y tener 9 dígitos", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Azul),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Azul)
            ) {
                Text(text = "Guardar y Continuar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}