package com.example.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxExporter {

    fun generateXlsxBytes(headers: List<String>, rows: List<List<String>>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            // 1. [Content_Types].xml
            val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""".trimIndent()
            addZipEntry(zos, "[Content_Types].xml", contentTypesXml)

            // 2. _rels/.rels
            val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent()
            addZipEntry(zos, "_rels/.rels", relsXml)

            // 3. xl/workbook.xml
            val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Yamaha Patrol Report" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".trimIndent()
            addZipEntry(zos, "xl/workbook.xml", workbookXml)

            // 4. xl/_rels/workbook.xml.rels
            val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".trimIndent()
            addZipEntry(zos, "xl/_rels/workbook.xml.rels", workbookRelsXml)

            // 5. xl/styles.xml - Strict OpenXML format required by MS Excel
            val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="1"><font><sz val="11"/><color theme="1"/><name val="Calibri"/><family val="2"/></font></fonts>
  <fills count="2"><fill><patternFill fillType="none"/></fill><fill><patternFill fillType="gray125"/></fill></fills>
  <borders count="1"><border><left/><right/><top/><bottom/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>""".trimIndent()
            addZipEntry(zos, "xl/styles.xml", stylesXml)

            // 6. xl/worksheets/sheet1.xml
            val sheetBuilder = StringBuilder()
            sheetBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sheetBuilder.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            sheetBuilder.append("""<sheetData>""")

            // Header Row (Row 1)
            var rowIndex = 1
            sheetBuilder.append("""<row r="$rowIndex">""")
            headers.forEachIndexed { colIndex, header ->
                val colName = getColumnName(colIndex)
                sheetBuilder.append("""<c r="$colName$rowIndex" t="inlineStr"><is><t>${escapeXml(header)}</t></is></c>""")
            }
            sheetBuilder.append("""</row>""")

            // Data Rows
            rows.forEach { rowData ->
                rowIndex++
                sheetBuilder.append("""<row r="$rowIndex">""")
                rowData.forEachIndexed { colIndex, cellValue ->
                    val colName = getColumnName(colIndex)
                    sheetBuilder.append("""<c r="$colName$rowIndex" t="inlineStr"><is><t>${escapeXml(cellValue)}</t></is></c>""")
                }
                sheetBuilder.append("""</row>""")
            }

            sheetBuilder.append("""</sheetData>""")
            sheetBuilder.append("""</worksheet>""")

            addZipEntry(zos, "xl/worksheets/sheet1.xml", sheetBuilder.toString())
        }
        return bos.toByteArray()
    }

    private fun addZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun getColumnName(colIndex: Int): String {
        var temp = colIndex
        val sb = StringBuilder()
        while (temp >= 0) {
            sb.insert(0, ('A'.code + (temp % 26)).toChar())
            temp = (temp / 26) - 1
        }
        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun exportToDownloads(context: Context, fileName: String, headers: List<String>, rows: List<List<String>>): Uri? {
        return try {
            val bytes = generateXlsxBytes(headers, rows)
            val mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(bytes)
                        os.flush()
                    }
                    uri
                } else null
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { os ->
                    os.write(bytes)
                    os.flush()
                }
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Log.e("XlsxExporter", "Excel export failed", e)
            null
        }
    }

    fun openExportedFile(context: Context, fileUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Excel Report"))
        } catch (e: Exception) {
            Log.w("XlsxExporter", "Could not launch file viewer intent", e)
        }
    }
}
