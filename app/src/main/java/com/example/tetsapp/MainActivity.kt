package com.example.tetsapp

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.tetsapp.data.Admin
import com.example.tetsapp.data.AppDatabase
import com.example.tetsapp.data.Report
import com.example.tetsapp.data.User
import com.example.tetsapp.ui.AdminHomeScreen
import com.example.tetsapp.ui.AdminLoginScreen
import com.example.tetsapp.ui.AdminSignUpScreen
import com.example.tetsapp.ui.HomeScreen
import com.example.tetsapp.ui.LoginScreen
import com.example.tetsapp.ui.ReportFormScreen
import com.example.tetsapp.ui.SignUpScreen
import com.example.tetsapp.ui.theme.TetsAppTheme
import kotlinx.coroutines.launch
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            TetsAppTheme(darkTheme = isDarkTheme) {
                var currentScreen by remember { mutableStateOf("login") }
                var selectedAuthority by remember { mutableStateOf("") }
                
                var loggedInUserPhone by remember { mutableStateOf("") }
                var loggedInAdminName by remember { mutableStateOf("") }
                var loggedInAdminDept by remember { mutableStateOf("") }
                var adminReports by remember { mutableStateOf(listOf<Report>()) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        "login" -> LoginScreen(
                            onLoginClick = { phone, password ->
                                lifecycleScope.launch {
                                    val user = database.userDao().getUserByPhone(phone)
                                    if (user != null && user.password == password) {
                                        loggedInUserPhone = phone
                                        currentScreen = "home"
                                    } else {
                                        Toast.makeText(this@MainActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onSignUpClick = {
                                currentScreen = "signup"
                            },
                            onAdminLoginClick = {
                                currentScreen = "admin_login"
                            },
                            onForgotPasswordClick = {
                                Toast.makeText(this@MainActivity, "Password reset feature coming soon", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "signup" -> SignUpScreen(
                            onSignUpClick = { name, address, phone, password ->
                                lifecycleScope.launch {
                                    try {
                                        database.userDao().insertUser(
                                            User(phone, name, address, password)
                                        )
                                        Toast.makeText(this@MainActivity, "Account created!", Toast.LENGTH_SHORT).show()
                                        currentScreen = "login"
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "User already exists or error occurred", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onBackToLoginClick = {
                                currentScreen = "login"
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "admin_login" -> AdminLoginScreen(
                            onLoginClick = { forceNumber, password ->
                                lifecycleScope.launch {
                                    val admin = database.adminDao().getAdminByForceNumber(forceNumber)
                                    if (admin != null && admin.password == password) {
                                        loggedInAdminName = admin.name
                                        loggedInAdminDept = admin.department
                                        adminReports = database.reportDao().getReportsByDepartment(admin.department)
                                        currentScreen = "admin_home"
                                    } else {
                                        Toast.makeText(this@MainActivity, "Invalid Admin credentials", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onSignUpClick = {
                                currentScreen = "admin_signup"
                            },
                            onBackClick = {
                                currentScreen = "login"
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "admin_signup" -> AdminSignUpScreen(
                            onSignUpClick = { name, forceNumber, department, phone, password ->
                                lifecycleScope.launch {
                                    try {
                                        database.adminDao().insertAdmin(
                                            Admin(forceNumber, name, department, password, phone)
                                        )
                                        Toast.makeText(this@MainActivity, "Admin account created!", Toast.LENGTH_SHORT).show()
                                        currentScreen = "admin_login"
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Admin already exists or error occurred", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onBackToLoginClick = {
                                currentScreen = "admin_login"
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "home" -> HomeScreen(
                            onReportOptionClick = { option ->
                                selectedAuthority = option
                                currentScreen = "report"
                            },
                            onLogoutClick = {
                                currentScreen = "login"
                            },
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { isDarkTheme = !isDarkTheme },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "admin_home" -> {
                            AdminHomeScreen(
                                adminName = loggedInAdminName,
                                department = loggedInAdminDept,
                                reports = adminReports,
                                onLogoutClick = { currentScreen = "login" },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        "report" -> ReportFormScreen(
                            authority = selectedAuthority,
                            onSubmit = { description, image ->
                                lifecycleScope.launch {
                                    database.reportDao().insertReport(
                                        Report(
                                            description = description,
                                            department = selectedAuthority,
                                            reporterPhone = loggedInUserPhone
                                        )
                                    )
                                    Toast.makeText(this@MainActivity, "Report submitted to $selectedAuthority", Toast.LENGTH_SHORT).show()
                                    currentScreen = "home"
                                }
                            },
                            onBack = {
                                currentScreen = "home"
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    TetsAppTheme {
        LoginScreen(
            onLoginClick = { _, _ -> },
            onSignUpClick = {},
            onAdminLoginClick = {},
            onForgotPasswordClick = {}
        )
    }
}