package com.example.agendatepe

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.agendatepe.presentation.login.SingUpScreen
import com.example.agendatepe.presentation.initial.InitialScreen
import com.example.agendatepe.presentation.login.LoginScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavigationWrapper(navHostController: NavHostController, auth: FirebaseAuth) {

    NavHost(navController = navHostController, startDestination = "initialScreen") {
        composable("initialScreen") {
            InitialScreen(
                navigateToLogin = { navHostController.navigate("logIn") },
                navigateToSignUp = { navHostController.navigate("SignUp") }

            )
        }
        composable("logIn"){
            LoginScreen(auth)
        }
        composable("SignUp"){
            SingUpScreen(auth)
        }
    }
}