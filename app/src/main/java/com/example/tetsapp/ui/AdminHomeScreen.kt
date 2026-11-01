package com.example.tetsapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tetsapp.R
import com.example.tetsapp.data.Report
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    adminName: String,
    department: String,
    reports: List<Report>,
    onLogoutClick: () -> Unit,
    onUpdateStatus: (Report, String) -> Unit,
    onViewMap: (Report) -> Unit,
    onRegisterAdminClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Dynamic Backgrounds
        val bgRes = when (department) {
            "ZINWA" -> R.drawable.zinwa_background
            "CITY COUNCIL" -> R.drawable.city_background
            "ZESA" -> R.drawable.zesa_background
            else -> null
        }
        
        bgRes?.let {
            Image(
                painter = painterResource(id = it),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.2f
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin: $adminName ($department)") },
                    actions = {
                        TextButton(onClick = onLogoutClick) {
                            Text("Logout", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (department == "Master") {
                    Button(
                        onClick = onRegisterAdminClick ?: {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Register New Admin Account")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Dashboard Stats
                AdminDashboardStats(reports)
                
                Spacer(modifier = Modifier.height(16.dp))

                if (reports.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No reports found for $department.")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(reports) { report ->
                            AdminReportCard(
                                report = report,
                                onStatusChange = { newStatus -> onUpdateStatus(report, newStatus) },
                                onMapClick = { onViewMap(report) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDashboardStats(reports: List<Report>) {
    val pending = reports.count { it.status == "Pending" }
    val inProgress = reports.count { it.status == "In Progress" }
    val resolved = reports.count { it.status == "Resolved" }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Pending", pending, Color(0xFFFFA500), Modifier.weight(1f))
        StatCard("In Progress", inProgress, Color(0xFF2196F3), Modifier.weight(1f))
        StatCard("Resolved", resolved, Color(0xFF4CAF50), Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, count: Int, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = count.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun AdminReportCard(
    report: Report,
    onStatusChange: (String) -> Unit,
    onMapClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Reporter: ${report.reporterPhone}", fontWeight = FontWeight.Bold)
                if (report.latitude != null) {
                    IconButton(onClick = onMapClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = "View Map", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = report.description)

            if (report.imageUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = report.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = "Current Status: ${report.status}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onStatusChange("In Progress") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) {
                    Text("Start", fontSize = 10.sp)
                }
                Button(onClick = { onStatusChange("Resolved") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                    Text("Resolve", fontSize = 10.sp)
                }
            }
        }
    }
}
