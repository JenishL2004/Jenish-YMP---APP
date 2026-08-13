package com.example.ui.screens

import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.content.Context
import android.os.Build
import android.os.Environment
import android.content.ContentValues
import android.provider.MediaStore
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.print.PrintDocumentInfo
import android.os.CancellationSignal
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PatrolLogEntity
import com.example.data.UserEntity
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusAbnormal
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.YamahaBlue
import com.example.ui.theme.YamahaRed

private fun saveCsvToDownloads(context: Context, fileName: String, content: String): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { os ->
                    os.write(content.toByteArray(Charsets.UTF_8))
                }
                true
            } else false
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            file.writeText(content, Charsets.UTF_8)
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun printPatrolPdf(context: Context, log: PatrolLogEntity) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        val jobName = "Yamaha_Patrol_Report_${log.patrolNumber}"
        printManager?.print(
            jobName,
            object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder("$jobName.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    val pdfDocument = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    val paint = Paint()

                    // Header
                    paint.color = AndroidColor.RED
                    paint.textSize = 18f
                    paint.isFakeBoldText = true
                    canvas.drawText("YAMAHA MOTOR INDIA - WELDING PATROL REPORT", 40f, 50f, paint)

                    paint.color = AndroidColor.BLACK
                    paint.textSize = 12f
                    paint.isFakeBoldText = false
                    canvas.drawText("Patrol Number: ${log.patrolNumber}", 40f, 90f, paint)
                    canvas.drawText("Shop / Line: ${log.shopName} / ${log.lineName}", 40f, 110f, paint)
                    canvas.drawText("Machine: ${log.machineName}", 40f, 130f, paint)
                    canvas.drawText("Inspector: ${log.employeeName} (${log.employeeId})", 40f, 150f, paint)
                    canvas.drawText("Shift: ${log.shift}", 40f, 170f, paint)
                    canvas.drawText("Overall Status: ${log.overallStatus}", 40f, 190f, paint)
                    canvas.drawText("Notes: ${log.notes}", 40f, 210f, paint)

                    canvas.drawText("Signatures:", 40f, 260f, paint)
                    canvas.drawText("Inspector: ____________________", 40f, 300f, paint)
                    canvas.drawText("Supervisor: ___________________", 300f, 300f, paint)

                    pdfDocument.finishPage(page)

                    try {
                        destination?.let { pfd ->
                            java.io.FileOutputStream(pfd.fileDescriptor).use { os ->
                                pdfDocument.writeTo(os)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    } finally {
                        pdfDocument.close()
                    }
                }
            },
            null
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun ReportsScreen(
    user: UserEntity,
    patrolLogs: List<PatrolLogEntity>,
    onGenerateExport: (reportType: String) -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedReportType by remember { mutableStateOf("DAILY") }
    var exportPreviewContent by remember { mutableStateOf<String?>(null) }
    var showPdfPreviewDialog by remember { mutableStateOf<PatrolLogEntity?>(null) }
    var showExcelPreviewDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header & Export Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Patrol Analytics & Enterprise Reports",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Yamaha Motor India Official Export System",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { showExcelPreviewDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6F42)), // Excel Green
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("action_export_excel")
                ) {
                    Icon(imageVector = Icons.Default.TableChart, contentDescription = "Excel", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Excel Export", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = {
                        val csv = onGenerateExport(selectedReportType)
                        saveCsvToDownloads(context, "Yamaha_Patrol_Export_${selectedReportType}.csv", csv)
                        exportPreviewContent = csv
                        Toast.makeText(context, "CSV saved to Downloads", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("action_export_csv")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Export", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "CSV Data", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Report Type Selector Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val reportTypes = listOf("DAILY", "SHIFT_WISE", "PLANT_WISE", "MACHINE_WISE", "EMPLOYEE_WISE")
            items(reportTypes) { type ->
                FilterChip(
                    selected = selectedReportType == type,
                    onClick = { selectedReportType = type },
                    label = { Text(type.replace("_", " ")) },
                    modifier = Modifier.testTag("report_type_$type")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Report Records List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(patrolLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = log.patrolNumber,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = YamahaBlue)
                                )
                                Text(
                                    text = "Machine: ${log.machineName}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            StatusBadge(status = log.overallStatus)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Shop: ${log.shopName} | Line: ${log.lineName}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                        )
                        Text(
                            text = "Inspector: ${log.employeeName} (${log.employeeId}) | Shift: ${log.shift}",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "Log Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(log.timestamp))}",
                            style = MaterialTheme.typography.labelSmall.copy(color = YamahaBlue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action: Print / Official PDF Document
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { showPdfPreviewDialog = log },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("view_pdf_button_${log.id}")
                            ) {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = YamahaRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Print Official Yamaha PDF Document", fontSize = 11.sp, color = YamahaRed)
                            }
                        }
                    }
                }
            }
        }
    }

    // Official Yamaha PDF Preview Modal
    showPdfPreviewDialog?.let { log ->
        AlertDialog(
            onDismissRequest = { showPdfPreviewDialog = null },
            confirmButton = {
                Button(
                    onClick = {
                        printPatrolPdf(context, log)
                        showPdfPreviewDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue)
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = "Print")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print / Download Document")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfPreviewDialog = null }) {
                    Text("Close")
                }
            },
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    // Official Header with Logo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_yamaha_hero_1785404947296),
                            contentDescription = "Yamaha Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(100.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "YAMAHA MOTOR INDIA", fontWeight = FontWeight.Black, fontSize = 14.sp, color = YamahaRed)
                            Text(text = "WELDING PATROL INSPECTION REPORT", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = YamahaBlue)
                            Text(text = "DOC NO: YMH-WPR-${log.id}-2026", fontSize = 9.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(YamahaBlue))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Employee & Patrol Details Section
                    Text(text = "1. EMPLOYEE & PATROL DETAILS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = YamahaBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F7FA))
                            .padding(8.dp)
                    ) {
                        Text(text = "Inspector Name: ${log.employeeName} (${log.employeeId})", fontSize = 11.sp)
                        Text(text = "Shop: ${log.shopName}", fontSize = 11.sp)
                        Text(text = "Line & Machine: ${log.lineName} | ${log.machineName}", fontSize = 11.sp)
                        Text(text = "Shift & Timestamp: ${log.shift} | ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(log.timestamp))}", fontSize = 11.sp)
                        Text(text = "Patrol Number: ${log.patrolNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Inspection Results & Abnormality Images
                    Text(text = "2. CHECKPOINT RESULTS & EVIDENCES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = YamahaBlue)
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray)
                            .padding(8.dp)
                    ) {
                        Text(text = "Overall Status: ${log.overallStatus}", fontWeight = FontWeight.Bold, color = if (log.overallStatus == "NORMAL") StatusNormal else StatusAbnormal, fontSize = 12.sp)
                        Text(text = "Inspector Remarks: ${log.notes}", fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = if (log.overallStatus == "ABNORMAL") "Abnormality recorded during patrol round" else "All checkpoints verified normal", fontSize = 10.sp, color = Color.DarkGray)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Corrective Action & Signature Section
                    Text(text = "3. AUTHORIZATION & SIGNATURES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = YamahaBlue)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.LightGray)
                                .padding(10.dp)
                        ) {
                            Text(text = log.employeeName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(text = "Inspector Signature", fontSize = 9.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(text = "[ VERIFIED ON-SITE ]", fontSize = 9.sp, color = StatusNormal, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.LightGray)
                                .padding(10.dp)
                        ) {
                            Text(text = "Plant Supervisor / Admin", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(text = "Quality Manager Approval", fontSize = 9.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(text = "[ SIGNED & APPROVED ]", fontSize = 9.sp, color = YamahaBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        )
    }

    // Excel Report Modal
    if (showExcelPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showExcelPreviewDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val headers = listOf("Patrol No", "Shop", "Line", "Machine", "Inspector", "Shift", "Status", "Date & Time", "Notes")
                        val rows = patrolLogs.map { log ->
                            listOf(
                                log.patrolNumber,
                                log.shopName,
                                log.lineName,
                                log.machineName,
                                log.employeeName,
                                log.shift,
                                log.overallStatus,
                                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(log.timestamp)),
                                log.notes
                            )
                        }
                        val fileName = "Yamaha_Patrol_Report_${System.currentTimeMillis()}.xlsx"
                        val exportedUri = com.example.data.XlsxExporter.exportToDownloads(context, fileName, headers, rows)
                        if (exportedUri != null) {
                            Toast.makeText(context, "Excel (.xlsx) saved to Downloads!", Toast.LENGTH_LONG).show()
                            com.example.data.XlsxExporter.openExportedFile(context, exportedUri)
                        } else {
                            Toast.makeText(context, "Excel export failed", Toast.LENGTH_SHORT).show()
                        }
                        showExcelPreviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6F42))
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Download Excel")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download .XLSX Workbook")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExcelPreviewDialog = false }) { Text("Close") }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.TableChart, contentDescription = "Excel", tint = Color(0xFF1D6F42))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Excel Report Preview", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Sheet: Yamaha_Patrol_Report.xlsx (${patrolLogs.size} records)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1D6F42))
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (patrolLogs.isEmpty()) {
                        Text(
                            text = "No patrol records found to export.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        patrolLogs.take(5).forEach { log ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7F2)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "${log.patrolNumber} | ${log.machineName}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(text = "Shop: ${log.shopName} | Line: ${log.lineName} | Status: ${log.overallStatus}", fontSize = 10.sp)
                                        Text(text = "Inspector: ${log.employeeName} (${log.shift})", fontSize = 9.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // Export CSV Preview Modal
    exportPreviewContent?.let { content ->
        AlertDialog(
            onDismissRequest = { exportPreviewContent = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.TableChart, contentDescription = "CSV", tint = YamahaBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Exported CSV Data ($selectedReportType)", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(text = "Raw CSV saved to downloads folder:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { exportPreviewContent = null }, colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue)) {
                    Text("OK / Done")
                }
            }
        )
    }
}
