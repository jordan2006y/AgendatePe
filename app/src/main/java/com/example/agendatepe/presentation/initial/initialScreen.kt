package com.example.agendatepe.presentation.initial

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendatepe.R
import com.example.agendatepe.ui.theme.BackgroundButton
import com.example.agendatepe.ui.theme.shapeButton
import com.example.agendatepe.ui.theme.Azul // Asegúrate de que Azul está definido
import com.example.agendatepe.ui.theme.Crema

// ----------------------------------------------------------------------------------
// FUNCIÓN PRINCIPAL DE LA PANTALLA
// ----------------------------------------------------------------------------------


@Preview
@Composable
fun InitialScreen(navigateToLogin: () -> Unit = {}, navigateToSignUp: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Azul),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. ESPACIADOR SUPERIOR
        Spacer(modifier = Modifier.weight(1f))

        // 2. LOGO (AgendatePé)
        Image(
            painter = painterResource(id = R.drawable.ic_logo_inicio),
            contentDescription = "Logo AgendatePé",
            modifier = Modifier.size(200.dp).padding(bottom = 32.dp)
        )

        // 3. IMAGEN PRINCIPAL DEL VENDEDOR/CASA
        // (Asegúrate de que R.drawable.seller_house_graphic exista)
        Image(
            painter = painterResource(id = R.drawable.ic_inicio),
            contentDescription = "Agente inmobiliario frente a casa",
            modifier = Modifier.size(300.dp)
        )

        // 4. ESPACIADOR CENTRAL
        Spacer(modifier = Modifier.weight(1f))

        // --- BOTONES SOLICITADOS EN EL ORDEN Y FLUJO CORREGIDO ---

        // Botón 1: INICIAR SESIÓN (Botón Primario de Correo/Contraseña)
        // Usamos Button estándar con el color primario si no debe llevar icono.
        Button(
            onClick = { navigateToLogin() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Crema) // Cambia Color.Yellow por tu color primario deseado
        ) {
            Text(
                text = "Iniciar Sesión",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navigateToLogin() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Crema) // Cambia Color.Yellow por tu color primario deseado
        ) {
            Text(
                text = "Registrarse",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Botón 3: CONTINUAR CON GOOGLE
        CustomButton(
            modifier = Modifier.clickable { /* Lógica para iniciar con Google */ },
            painter = painterResource(id = R.drawable.google), // Ícono de Google
            title = "Continuar con Google"

        )

        // 5. ESPACIADOR INFERIOR
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun CustomButton(modifier: Modifier, painter: Painter, title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 32.dp)
            .background(color = Crema, shape = CircleShape)
            .border(2.dp, shapeButton, CircleShape)
            .then(modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 16.dp)
                .size(24.dp)
        )
        Text(
            text = title,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}