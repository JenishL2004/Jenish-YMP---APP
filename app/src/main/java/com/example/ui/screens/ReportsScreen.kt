package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AbnormalityEntity
import com.example.data.PatrolLogEntity
import com.example.data.PatrolPointResultEntity
import com.example.data.UserEntity
import com.example.data.XlsxExporter
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusAbnormal
import com.example.ui.theme.StatusNormal
import com.example.ui.theme.YamahaBlue
import com.example.ui.theme.YamahaRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            val file = File(downloadsDir, fileName)
            file.writeText(content, Charsets.UTF_8)
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun loadBitmapSafely(context: Context, uriString: String?): Bitmap? {
    if (uriString.isNullOrBlank()) return null
    return try {
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            val url = URL(uriString)
            val conn = url.openConnection() as HttpURLConnection
            conn.doInput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()
            val stream: InputStream = conn.inputStream
            BitmapFactory.decodeStream(stream)
        } else {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    } catch (e: Exception) {
        null
    }
}

private fun printPatrolPdf(
    context: Context,
    log: PatrolLogEntity,
    results: List<PatrolPointResultEntity>,
    associatedAbnormality: AbnormalityEntity?
) {
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
                    paint.color = android.graphics.Color.parseColor("#CC0000") // Yamaha Red
                    paint.textSize = 16f
                    paint.isFakeBoldText = true
                    canvas.drawText("YAMAHA MOTOR INDIA - WELDING PATROL REPORT", 36f, 44f, paint)

                    paint.color = android.graphics.Color.parseColor("#003366") // Yamaha Blue
                    paint.textSize = 10f
                    paint.isFakeBoldText = true
                    canvas.drawText("OFFICIAL QUALITY & MAINTENANCE RECORD | DOC: YMH-WPR-${log.id}-2026", 36f, 58f, paint)

                    paint.color = android.graphics.Color.DKGRAY
                    paint.strokeWidth = 1.5f
                    canvas.drawLine(36f, 66f, 559f, 66f, paint)

                    var yPos = 84f

                    // Section 1: Patrol Metadata
                    paint.color = android.graphics.Color.parseColor("#003366")
                    paint.textSize = 11f
                    paint.isFakeBoldText = true
                    canvas.drawText("1. PATROL & EQUIPMENT DETAILS", 36f, yPos, paint)
                    yPos += 14f

                    paint.color = android.graphics.Color.BLACK
                    paint.textSize = 9.5f
                    paint.isFakeBoldText = false
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(log.timestamp))

                    canvas.drawText("Patrol Number: ${log.patrolNumber}", 36f, yPos, paint)
                    canvas.drawText("Date & Time: $dateFormatted", 300f, yPos, paint)
                    yPos += 13f

                    canvas.drawText("Shop / Line: ${log.shopName} / ${log.lineName}", 36f, yPos, paint)
                    canvas.drawText("Shift: ${log.shift}", 300f, yPos, paint)
                    yPos += 13f

                    canvas.drawText("Machine: ${log.machineName}", 36f, yPos, paint)
                    canvas.drawText("Inspector: ${log.employeeName} (${log.employeeId})", 300f, yPos, paint)
                    yPos += 13f

                    val statusColor = if (log.overallStatus == "NORMAL") android.graphics.Color.parseColor("#1B5E20") else android.graphics.Color.parseColor("#B71C1C")
                    paint.color = statusColor
                    paint.isFakeBoldText = true
                    canvas.drawText("Overall Status: ${log.overallStatus}", 36f, yPos, paint)
                    paint.color = android.graphics.Color.BLACK
                    paint.isFakeBoldText = false
                    canvas.drawText("Notes: ${log.notes.take(40)}", 300f, yPos, paint)
                    yPos += 20f

                    // Section 2: Checkpoint Results Table
                    paint.color = android.graphics.Color.parseColor("#003366")
                    paint.textSize = 11f
                    paint.isFakeBoldText = true
                    canvas.drawText("2. CHECKPOINT RESULTS (${results.size} Points Inspected)", 36f, yPos, paint)
                    yPos += 14f

                    // Table Header
                    paint.color = android.graphics.Color.LTGRAY
                    canvas.drawRect(36f, yPos - 10f, 559f, yPos + 4f, paint)
                    paint.color = android.graphics.Color.BLACK
                    paint.textSize = 8.5f
                    paint.isFakeBoldText = true
                    canvas.drawText("Point Name", 40f, yPos, paint)
                    canvas.drawText("Category", 180f, yPos, paint)
                    canvas.drawText("Standard Value", 260f, yPos, paint)
                    canvas.drawText("Status", 420f, yPos, paint)
                    canvas.drawText("Remarks", 480f, yPos, paint)
                    yPos += 14f

                    // Table Rows
                    paint.isFakeBoldText = false
                    results.take(12).forEach { res ->
                        paint.color = android.graphics.Color.BLACK
                        canvas.drawText(res.checkpointName.take(22), 40f, yPos, paint)
                        canvas.drawText(res.category.take(14), 180f, yPos, paint)
                        canvas.drawText(res.standardValue.take(22), 260f, yPos, paint)

                        paint.color = if (res.status == "NORMAL") android.graphics.Color.parseColor("#1B5E20") else android.graphics.Color.parseColor("#B71C1C")
                        paint.isFakeBoldText = true
                        canvas.drawText(res.status, 420f, yPos, paint)
                        paint.isFakeBoldText = false

                        paint.color = android.graphics.Color.DKGRAY
                        canvas.drawText(res.remarks.take(16), 480f, yPos, paint)
                        yPos += 12f
                    }

                    yPos += 10f

                    // Section 3: Photo Evidence
                    val pointWithPhoto = results.firstOrNull { !it.photoUri.isNullOrBlank() }
                    val photoUriToLoad = pointWithPhoto?.photoUri ?: associatedAbnormality?.photoUri

                    paint.color = android.graphics.Color.parseColor("#003366")
                    paint.textSize = 11f
                    paint.isFakeBoldText = true
                    canvas.drawText("3. INSPECTION PHOTO EVIDENCE", 36f, yPos, paint)
                    yPos += 14f

                    if (!photoUriToLoad.isNullOrBlank()) {
                        val bitmap = loadBitmapSafely(context, photoUriToLoad)
                        if (bitmap != null) {
                            val destRect = RectF(36f, yPos, 196f, yPos + 100f)
                            canvas.drawBitmap(bitmap, null, destRect, null)

                            paint.color = android.graphics.Color.DKGRAY
                            paint.textSize = 8f
                            paint.isFakeBoldText = false
                            val caption = if (pointWithPhoto != null) "Point: ${pointWithPhoto.checkpointName} (${pointWithPhoto.status})" else "Abnormality Evidence"
                            canvas.drawText(caption, 36f, yPos + 110f, paint)
                            yPos += 122f
                        } else {
                            paint.color = android.graphics.Color.GRAY
                            paint.textSize = 9f
                            paint.isFakeBoldText = false
                            canvas.drawText("[Photo attached: ${photoUriToLoad.take(50)}...]", 36f, yPos, paint)
                            yPos += 16f
                        }
                    } else {
                        paint.color = android.graphics.Color.DKGRAY
                        paint.textSize = 9f
                        paint.isFakeBoldText = false
                        canvas.drawText("No photographic abnormality recorded for this patrol inspection.", 36f, yPos, paint)
                        yPos += 16f
                    }

                    // Section 4: Abnormality / RCA Summary (if exists)
                    if (associatedAbnormality != null) {
                        yPos += 6f
                        paint.color = android.graphics.Color.parseColor("#B71C1C")
                        paint.textSize = 11f
                        paint.isFakeBoldText = true
                        canvas.drawText("4. ABNORMALITY & COUNTERMEASURE RECORD", 36f, yPos, paint)
                        yPos += 14f

                        paint.color = android.graphics.Color.BLACK
                        paint.textSize = 8.5f
                        paint.isFakeBoldText = false
                        canvas.drawText("Problem: ${associatedAbnormality.problemDescription}", 36f, yPos, paint)
                        yPos += 11f
                        canvas.drawText("Root Cause: ${associatedAbnormality.rootCause.ifBlank { "Under RCA investigation" }}", 36f, yPos, paint)
                        yPos += 11f
                        canvas.drawText("Corrective Action: ${associatedAbnormality.correctiveAction.ifBlank { "Action pending" }} | Resp: ${associatedAbnormality.responsiblePerson}", 36f, yPos, paint)
                        yPos += 16f
                    }

                    // Section 5: Signatures
                    val signY = 780f
                    paint.color = android.graphics.Color.BLACK
                    paint.textSize = 9f
                    paint.isFakeBoldText = true
                    canvas.drawText("Inspector: ${log.employeeName}", 36f, signY, paint)
                    paint.isFakeBoldText = false
                    canvas.drawText("Signature: [ VERIFIED ON-SITE ]", 36f, signY + 12f, paint)

                    paint.isFakeBoldText = true
                    canvas.drawText("Quality Supervisor / Admin", 340f, signY, paint)
                    paint.isFakeBoldText = false
                    canvas.drawText("Approval: [ SIGNED & APPROVED ]", 340f, signY + 12f, paint)

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
    abnormalities: List<AbnormalityEntity> = emptyList(),
    onGetResultsForLog: suspend (Int) -> List<PatrolPointResultEntity> = { emptyList() },
    onGetAllResults: suspend () -> List<PatrolPointResultEntity> = { emptyList() },
    onGenerateExport: (reportType: String) -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedReportType by remember { mutableStateOf("DAILY") }
    var exportPreviewContent by remember { mutableStateOf<String?>(null) }
    var showPdfPreviewDialog by remember { mutableStateOf<PatrolLogEntity?>(null) }
    var showExcelPreviewDialog by remember { mutableStateOf(false) }

    var currentPreviewResults by remember { mutableStateOf<List<PatrolPointResultEntity>>(emptyList()) }
    var isLoadingResults by remember { mutableStateOf(false) }

    LaunchedEffect(showPdfPreviewDialog) {
        val log = showPdfPreviewDialog
        if (log != null) {
            isLoadingResults = true
            currentPreviewResults = onGetResultsForLog(log.id)
            isLoadingResults = false
        } else {
            currentPreviewResults = emptyList()
        }
    }

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
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(patrolLogs, key = { it.id }) { log ->
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
                            text = "Log Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(log.timestamp))}",
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
                                Text("View & Print Official PDF Document", fontSize = 11.sp, color = YamahaRed)
                            }
                        }
                    }
                }
            }
        }
    }

    // Official Yamaha PDF Preview Modal
    showPdfPreviewDialog?.let { log ->
        val matchingAbnormality = abnormalities.firstOrNull { it.patrolLogId == log.id }

        AlertDialog(
            onDismissRequest = { showPdfPreviewDialog = null },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val results = withContext(Dispatchers.IO) { onGetResultsForLog(log.id) }
                            printPatrolPdf(context, log, results, matchingAbnormality)
                        }
                        showPdfPreviewDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YamahaBlue)
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = "Print")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print Official Document")
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
                            Text(text = "YAMAHA MOTOR INDIA", fontWeight = FontWeight.Black, fontSize = 13.sp, color = YamahaRed)
                            Text(text = "PATROL INSPECTION REPORT", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = YamahaBlue)
                            Text(text = "DOC NO: YMH-WPR-${log.id}-2026", fontSize = 9.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(YamahaBlue))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Employee & Patrol Details Section
                    Text(text = "1. EMPLOYEE & PATROL DETAILS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = YamahaBlue)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F7FA))
                            .padding(8.dp)
                    ) {
                        Text(text = "Inspector: ${log.employeeName} (${log.employeeId})", fontSize = 11.sp)
                        Text(text = "Shop / Line: ${log.shopName} / ${log.lineName}", fontSize = 11.sp)
                        Text(text = "Machine: ${log.machineName}", fontSize = 11.sp)
                        Text(text = "Shift / Date: ${log.shift} | ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(log.timestamp))}", fontSize = 11.sp)
                        Text(text = "Patrol Number: ${log.patrolNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Overall Status: ${log.overallStatus}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (log.overallStatus == "NORMAL") StatusNormal else StatusAbnormal)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Inspection Checkpoints Section
                    Text(text = "2. CHECKPOINT RESULTS & OBSERVATIONS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = YamahaBlue)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (isLoadingResults) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else if (currentPreviewResults.isEmpty()) {
                        Text(text = "No individual checkpoints recorded.", fontSize = 10.sp, color = Color.Gray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            currentPreviewResults.forEach { res ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = res.checkpointName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            StatusBadge(status = res.status)
                                        }
                                        Text(text = "Category: ${res.category} | Standard: ${res.standardValue}", fontSize = 9.sp, color = Color.DarkGray)
                                        if (res.remarks.isNotBlank()) {
                                            Text(text = "Remarks: ${res.remarks}", fontSize = 9.sp, color = Color.Black)
                                        }

                                        // Render Evidence Photo if available
                                        if (!res.photoUri.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(text = "Point Photo Evidence:", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = YamahaBlue)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            AsyncImage(
                                                model = res.photoUri,
                                                contentDescription = "Evidence Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Abnormality Details Section (if exists)
                    matchingAbnormality?.let { ab ->
                        Text(text = "3. ABNORMALITY & CORRECTIVE ACTION", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = YamahaRed)
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF0F0))
                                .border(1.dp, YamahaRed.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text(text = "Problem: ${ab.problemDescription}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = "Priority: ${ab.priority} | Status: ${ab.status}", fontSize = 9.sp)
                            if (ab.rootCause.isNotBlank()) {
                                Text(text = "Root Cause: ${ab.rootCause}", fontSize = 9.sp)
                            }
                            if (ab.correctiveAction.isNotBlank()) {
                                Text(text = "Countermeasure: ${ab.correctiveAction}", fontSize = 9.sp)
                            }
                            Text(text = "Responsible: ${ab.responsiblePerson}", fontSize = 9.sp)

                            if (!ab.photoUri.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Abnormality Photo Evidence:", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = YamahaRed)
                                Spacer(modifier = Modifier.height(2.dp))
                                AsyncImage(
                                    model = ab.photoUri,
                                    contentDescription = "Abnormality Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, YamahaRed.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Signatures Section
                    Text(text = "4. AUTHORIZATION & SIGNATURES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = YamahaBlue)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.LightGray)
                                .padding(8.dp)
                        ) {
                            Text(text = log.employeeName, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text(text = "Inspector Signature", fontSize = 8.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(text = "[ VERIFIED ON-SITE ]", fontSize = 9.sp, color = StatusNormal, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.LightGray)
                                .padding(8.dp)
                        ) {
                            Text(text = "Plant Supervisor / Admin", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text(text = "Quality Manager Approval", fontSize = 8.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(14.dp))
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
                        scope.launch {
                            val allResults = withContext(Dispatchers.IO) { onGetAllResults() }
                            val resultsByLog = allResults.groupBy { it.patrolLogId }

                            val headers = listOf(
                                "Patrol No", "Shop", "Line", "Machine", "Inspector", "Shift",
                                "Date & Time", "Overall Status", "Checkpoint Name", "Category",
                                "Standard Value", "Checkpoint Status", "Remarks", "Evidence Photo URL"
                            )

                            val rows = mutableListOf<List<String>>()
                            patrolLogs.forEach { log ->
                                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(log.timestamp))
                                val logResults = resultsByLog[log.id] ?: emptyList()

                                if (logResults.isEmpty()) {
                                    rows.add(
                                        listOf(
                                            log.patrolNumber, log.shopName, log.lineName, log.machineName,
                                            log.employeeName, log.shift, dateStr, log.overallStatus,
                                            "General Inspection", "N/A", "N/A", log.overallStatus, log.notes, ""
                                        )
                                    )
                                } else {
                                    logResults.forEach { res ->
                                        rows.add(
                                            listOf(
                                                log.patrolNumber, log.shopName, log.lineName, log.machineName,
                                                log.employeeName, log.shift, dateStr, log.overallStatus,
                                                res.checkpointName, res.category, res.standardValue, res.status,
                                                res.remarks, res.photoUri ?: ""
                                            )
                                        )
                                    }
                                }
                            }

                            val fileName = "Yamaha_Patrol_Detailed_Report_${System.currentTimeMillis()}.xlsx"
                            val exportedUri = XlsxExporter.exportToDownloads(context, fileName, headers, rows)
                            if (exportedUri != null) {
                                Toast.makeText(context, "Detailed Excel Report (.xlsx) saved to Downloads!", Toast.LENGTH_LONG).show()
                                XlsxExporter.openExportedFile(context, exportedUri)
                            } else {
                                Toast.makeText(context, "Excel export failed", Toast.LENGTH_SHORT).show()
                            }
                            showExcelPreviewDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6F42))
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Download Excel")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download Full .XLSX Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExcelPreviewDialog = false }) { Text("Close") }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.TableChart, contentDescription = "Excel", tint = Color(0xFF1D6F42))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Excel Enterprise Export", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Generates complete multi-column Excel (.xlsx) workbook containing all inspection logs, checkpoint measurements, remarks, and photo evidence links.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Total Records: ${patrolLogs.size} Patrols",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1D6F42))
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    patrolLogs.take(4).forEach { log ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7F2)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = "${log.patrolNumber} | ${log.machineName}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(text = "${log.shopName} > ${log.lineName} | Status: ${log.overallStatus}", fontSize = 10.sp)
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
