package com.example.tetsapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.tetsapp.data.Admin
import com.example.tetsapp.data.User
import com.example.tetsapp.data.AppDatabase
import com.example.tetsapp.data.Report
import com.example.tetsapp.ui.*
import com.example.tetsapp.ui.theme.TetsAppTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val db = Firebase.firestore
        val storage = Firebase.storage
        
        val database = AppDatabase.getDatabase(this)

        lifecycleScope.launch {
            try {
                val masterAdmin = Admin(
                    forceNumber = "Tafadzwa",
                    name = "Tafadzwa",
                    department = "Master",
                    password = "123456789",
                    phoneNumber = "0777777777"
                )
                database.adminDao().insertAdmin(masterAdmin)

                val testUser = User(
                    phoneNumber = "0777777777",
                    name = "Tafadzwa",
                    address = "Harare",
                    password = "123456789"
                )
                database.userDao().insertUser(testUser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            TetsAppTheme(darkTheme = isDarkTheme) {
                var currentScreen by remember { mutableStateOf("login") }
                var selectedAuthority by remember { mutableStateOf("") }
                
                var loggedInUserPhone by remember { mutableStateOf("") }
                var loggedInAdminName by remember { mutableStateOf("") }
                var loggedInAdminDept by remember { mutableStateOf("") }
                
                var userReports by remember { mutableStateOf(listOf<Report>()) }
                var adminReports by remember { mutableStateOf(listOf<Report>()) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        "login" -> LoginScreen(
                            onLoginClick = { phone, password ->
                                lifecycleScope.launch {
                                    try {
                                        // Check Local Room first (Fast, works offline)
                                        val localUser = database.userDao().getUserByPhone(phone)
                                        if (localUser != null) {
                                            if (localUser.password == password) {
                                                loggedInUserPhone = phone
                                                currentScreen = "home"
                                                return@launch
                                            } else {
                                                Toast.makeText(this@MainActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }
                                        }

                                        // If not found locally, check Firestore
                                        try {
                                            val doc = db.collection("users").document(phone).get().await()
                                            if (doc.exists()) {
                                                if (doc.getString("password") == password) {
                                                    val name = doc.getString("name") ?: ""
                                                    val address = doc.getString("address") ?: ""
                                                    database.userDao().insertUser(User(phone, name, address, password))
                                                    loggedInUserPhone = phone
                                                    currentScreen = "home"
                                                } else {
                                                    Toast.makeText(this@MainActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(this@MainActivity, "Account not found. Please Sign Up.", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, "Account not found locally and cloud error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Login error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onSignUpClick = { currentScreen = "signup" },
                            onAdminLoginClick = { currentScreen = "admin_login" },
                            onForgotPasswordClick = { currentScreen = "forgot_password" },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "signup" -> SignUpScreen(
                            onSignUpClick = { name, address, phone, password ->
                                lifecycleScope.launch {
                                    try {
                                        val userObj = User(
                                            phoneNumber = phone,
                                            name = name,
                                            address = address,
                                            password = password
                                        )
                                        database.userDao().insertUser(userObj)

                                        try {
                                            val userMap = mapOf(
                                                "name" to name,
                                                "address" to address,
                                                "phoneNumber" to phone,
                                                "password" to password
                                            )
                                            db.collection("users").document(phone).set(userMap).await()
                                        } catch (e: Exception) {
                                            // Local save successful, cloud sync deferred
                                        }

                                        Toast.makeText(this@MainActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                        currentScreen = "login"
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onBackToLoginClick = { currentScreen = "login" },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "admin_login" -> AdminLoginScreen(
                            onLoginClick = { forceNumber, password ->
                                lifecycleScope.launch {
                                    try {
                                        // Check Local Room first (Fast, works offline)
                                        val localAdmin = database.adminDao().getAdminByForceNumber(forceNumber)
                                        if (localAdmin != null) {
                                            if (localAdmin.password == password) {
                                                loggedInAdminName = localAdmin.name
                                                loggedInAdminDept = localAdmin.department

                                                val query = if (loggedInAdminDept == "Master") {
                                                    db.collection("reports")
                                                } else {
                                                    db.collection("reports").whereEqualTo("department", loggedInAdminDept)
                                                }
                                                query.addSnapshotListener { snapshot, _ ->
                                                    if (snapshot != null) {
                                                        adminReports = snapshot.toObjects(Report::class.java)
                                                    }
                                                }
                                                currentScreen = "admin_home"
                                                return@launch
                                            } else {
                                                Toast.makeText(this@MainActivity, "Incorrect Admin password", Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }
                                        }

                                        // If not found locally, check Firestore
                                        try {
                                            val doc = db.collection("admins").document(forceNumber).get().await()
                                            if (doc.exists()) {
                                                if (doc.getString("password") == password) {
                                                    loggedInAdminName = doc.getString("name") ?: ""
                                                    loggedInAdminDept = doc.getString("department") ?: ""
                                                    
                                                    val newAdmin = Admin(
                                                        forceNumber = forceNumber,
                                                        name = loggedInAdminName,
                                                        department = loggedInAdminDept,
                                                        password = password,
                                                        phoneNumber = doc.getString("phoneNumber") ?: ""
                                                    )
                                                    database.adminDao().insertAdmin(newAdmin)

                                                val query = if (loggedInAdminDept == "Master") {
                                                    db.collection("reports")
                                                } else {
                                                    db.collection("reports").whereEqualTo("department", loggedInAdminDept)
                                                }
                                                query.addSnapshotListener { snapshot, _ ->
                                                    if (snapshot != null) {
                                                        adminReports = snapshot.toObjects(Report::class.java)
                                                    }
                                                }
                                                    currentScreen = "admin_home"
                                                } else {
                                                    Toast.makeText(this@MainActivity, "Incorrect Admin password", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(this@MainActivity, "Admin account not found. Please register.", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, "Admin account not found locally and cloud error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Login error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onBackClick = { currentScreen = "login" },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "admin_signup" -> AdminSignUpScreen(
                            onSignUpClick = { name, forceNumber, department, phone, password ->
                                lifecycleScope.launch {
                                    try {
                                        val adminObj = Admin(
                                            forceNumber = forceNumber,
                                            name = name,
                                            department = department,
                                            phoneNumber = phone,
                                            password = password
                                        )
                                        database.adminDao().insertAdmin(adminObj)

                                        try {
                                            val adminMap = mapOf(
                                                "name" to name,
                                                "forceNumber" to forceNumber,
                                                "department" to department,
                                                "phoneNumber" to phone,
                                                "password" to password
                                            )
                                            db.collection("admins").document(forceNumber).set(adminMap).await()
                                        } catch (e: Exception) {
                                            // Local save successful, cloud sync deferred
                                        }

                                        Toast.makeText(this@MainActivity, "Admin account registered successfully!", Toast.LENGTH_SHORT).show()
                                        if (loggedInAdminDept == "Master") {
                                            currentScreen = "admin_home"
                                        } else {
                                            currentScreen = "admin_login"
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onBackToLoginClick = { 
                                if (loggedInAdminDept == "Master") {
                                    currentScreen = "admin_home"
                                } else {
                                    currentScreen = "admin_login"
                                }
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "home" -> HomeScreen(
                            onReportOptionClick = { option ->
                                selectedAuthority = option
                                currentScreen = "report"
                            },
                            onViewMyReportsClick = {
                                lifecycleScope.launch {
                                    val snapshot = db.collection("reports")
                                        .whereEqualTo("reporterPhone", loggedInUserPhone)
                                        .get().await()
                                    userReports = snapshot.toObjects(Report::class.java)
                                    currentScreen = "my_reports"
                                }
                            },
                            onEmergencyCallClick = { number ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                startActivity(intent)
                            },
                            onLogoutClick = { currentScreen = "login" },
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { isDarkTheme = !isDarkTheme },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "my_reports" -> UserReportsScreen(
                            reports = userReports,
                            onBackClick = { currentScreen = "home" },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "admin_home" -> AdminHomeScreen(
                            adminName = loggedInAdminName,
                            department = loggedInAdminDept,
                            reports = adminReports,
                            onLogoutClick = { currentScreen = "login" },
                            onUpdateStatus = { report, newStatus ->
                                lifecycleScope.launch {
                                    // Using a query to find the doc since we don't store docId in the class yet
                                    val snapshot = db.collection("reports")
                                        .whereEqualTo("timestamp", report.timestamp)
                                        .get().await()
                                    if (!snapshot.isEmpty) {
                                        snapshot.documents[0].reference.update("status", newStatus).await()
                                        Toast.makeText(this@MainActivity, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onViewMap = { report ->
                                val gmmIntentUri = Uri.parse("geo:0,0?q=${report.latitude},${report.longitude}(Report Location)")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                startActivity(mapIntent)
                            },
                            onRegisterAdminClick = { currentScreen = "admin_signup" },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "report" -> ReportFormScreen(
                            authority = selectedAuthority,
                            onSubmit = { description, imageUri, lat, lon ->
                                lifecycleScope.launch {
                                    try {
                                        var finalImageUrl: String? = null
                                        if (imageUri != null) {
                                            val ref = storage.reference.child("reports/${UUID.randomUUID()}")
                                            ref.putFile(imageUri).await()
                                            finalImageUrl = ref.downloadUrl.await().toString()
                                        }

                                        val report = Report(
                                            description = description,
                                            department = selectedAuthority,
                                            reporterPhone = loggedInUserPhone,
                                            latitude = lat,
                                            longitude = lon,
                                            imageUrl = finalImageUrl
                                        )
                                        db.collection("reports").add(report).await()
                                        Toast.makeText(this@MainActivity, "Report sent successfully!", Toast.LENGTH_SHORT).show()
                                        currentScreen = "home"
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Submission failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onBack = { currentScreen = "home" },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "forgot_password" -> ForgotPasswordScreen(
                            onResetClick = { phone, newPassword ->
                                lifecycleScope.launch {
                                    try {
                                        db.collection("users").document(phone).update("password", newPassword).await()
                                        Toast.makeText(this@MainActivity, "Password updated!", Toast.LENGTH_SHORT).show()
                                        currentScreen = "login"
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onBackClick = { currentScreen = "login" },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
