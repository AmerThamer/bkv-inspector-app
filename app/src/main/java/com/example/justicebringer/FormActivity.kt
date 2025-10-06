package com.example.justicebringer

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import com.example.justicebringer.data.DataStorage
import com.example.justicebringer.databinding.ActivityFormBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class FormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormBinding
    private val drivers = mutableListOf<Driver>()
    private val routes = mutableListOf<Route>()
    private val inspectors = mutableListOf<Inspector>()

    private val positiveItems by lazy {
        listOf(
            getString(R.string.pozitiv_eszrevetel1),
            getString(R.string.pozitiv_eszrevetel2),
            getString(R.string.pozitiv_eszrevetel3),
            getString(R.string.pozitiv_eszrevetel4),
            getString(R.string.pozitiv_eszrevetel5),
            getString(R.string.pozitiv_eszrevetel6),
            getString(R.string.pozitiv_eszrevetel7),
            getString(R.string.pozitiv_eszrevetel8),
            getString(R.string.pozitiv_eszrevetel9),
            getString(R.string.pozitiv_eszrevetel10)
        )
    }

    private val negativeItems by lazy {
        listOf(
            getString(R.string.negativ_eszrevetel1),
            getString(R.string.negativ_eszrevetel2),
            getString(R.string.negativ_eszrevetel3),
            getString(R.string.negativ_eszrevetel4),
            getString(R.string.negativ_eszrevetel5),
            getString(R.string.negativ_eszrevetel6),
            getString(R.string.negativ_eszrevetel7),
            getString(R.string.negativ_eszrevetel8),
            getString(R.string.negativ_eszrevetel9),
            getString(R.string.negativ_eszrevetel10)
        )
    }

    private val selectedPositive = mutableSetOf<String>()
    private val selectedNegative = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadData()

        val inspectorAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            inspectors.map { "${it.name} (${it.code})" }
        )
        binding.autoInspector.setAdapter(inspectorAdapter)

        val driverNames = drivers.map { it.name }
        val driverCodes = drivers.map { it.code }

        val driverNameAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, driverNames)
        val driverCodeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, driverCodes)

        binding.autoDriverName.setAdapter(driverNameAdapter)
        binding.autoDriverCode.setAdapter(driverCodeAdapter)

        binding.autoDriverName.setOnItemClickListener { _, _, position, _ ->
            val name = driverNames[position]
            val driver = drivers.find { it.name == name }
            driver?.let { binding.autoDriverCode.setText(it.code, false) }
        }

        binding.autoDriverCode.setOnItemClickListener { _, _, position, _ ->
            val code = driverCodes[position]
            val driver = drivers.find { it.code == code }
            driver?.let { binding.autoDriverName.setText(it.name, false) }
        }

        binding.autoDriverName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val name = binding.autoDriverName.text.toString()
                val driver = drivers.find { it.name == name }
                if (driver != null) binding.autoDriverCode.setText(driver.code, false) else binding.autoDriverCode.text.clear()
            }
        }

        binding.autoDriverCode.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val code = binding.autoDriverCode.text.toString()
                val driver = drivers.find { it.code == code }
                if (driver != null) binding.autoDriverName.setText(driver.name, false) else binding.autoDriverName.text.clear()
            }
        }

        val allLines = routes.map { it.line.trim() }.distinct().sorted()
        val lineAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, allLines)
        binding.autoLine.setAdapter(lineAdapter)

        binding.autoLine.setOnItemClickListener { _, _, position, _ ->
            val selectedLine = allLines[position].trim()
            updateLocationsForLine(selectedLine)
        }

        binding.autoLine.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val typedLine = binding.autoLine.text.toString().trim()
                if (typedLine.isNotEmpty()) updateLocationsForLine(typedLine)
            }
        }

        setupTimeField(binding.editStartTime)
        setupTimeField(binding.editEndTime)

        setupMultiSelect(binding.txtPositive, positiveItems, selectedPositive)
        setupMultiSelect(binding.txtNegative, negativeItems, selectedNegative)

        binding.btnGeneratePdf.setOnClickListener {
            val inspector = binding.autoInspector.text.toString()
            val driverName = binding.autoDriverName.text.toString()
            val driverCode = binding.autoDriverCode.text.toString()
            val line = binding.autoLine.text.toString()
            val vehicleCode = binding.editVehicleCode.text.toString()
            val startLoc = binding.autoStartLocation.text.toString()
            val startTime = normalizeTime(binding.editStartTime.text.toString())
            val endLoc = binding.autoEndLocation.text.toString()
            val endTime = normalizeTime(binding.editEndTime.text.toString())
            val notes = binding.editNotes.text.toString()

            val date = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

            val positives = selectedPositive.toList()
            val negatives = selectedNegative.toList()

            val header = "Ellenőrzést végző személy: $inspector"

            val bodyText = """
                Mai napon $date a $line viszonylaton ellenőriztem a $vehicleCode pályaszámú járművel közlekedő $driverName ($driverCode) járművezetőt.
                A következő szakaszon: $startTime $startLoc - $endTime $endLoc.

                Következő megállapításokat tettem:
            """.trimIndent()



            val pdfFile = createPdfWithTwoColumns(header, bodyText, positives, negatives, notes, driverCode)

            val pdfUri = FileProvider.getUriForFile(this, "${packageName}.provider", pdfFile)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfUri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(viewIntent, "PDF megnyitása"))
        }
    }

    private fun setupTimeField(editText: android.widget.EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            var selfChange = false
            var prev = ""
            var prevSel = 0
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                prev = s?.toString() ?: ""
                prevSel = start + after
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (selfChange) return
                val raw = s?.toString() ?: ""
                val digits = raw.replace("[^\\d]".toRegex(), "")
                if (digits.isEmpty()) {
                    return
                }
                val limited = if (digits.length > 4) digits.substring(0, 4) else digits
                val hStr = when {
                    limited.length >= 2 -> limited.substring(0, 2)
                    else -> limited
                }
                val mStr = if (limited.length > 2) limited.substring(2) else ""
                var h = if (hStr.isNotEmpty()) hStr.toInt() else 0
                if (h > 23) h = 23
                var out = if (hStr.length >= 2) String.format("%02d", h) else hStr
                if (out.length >= 2) {
                    out += ":"
                    if (mStr.isNotEmpty()) {
                        if (mStr.length >= 2) {
                            var m = mStr.substring(0, 2).toInt()
                            if (m > 59) m = 59
                            out += String.format("%02d", m)
                        } else {
                            out += mStr
                        }
                    }
                }
                if (out == raw) return
                selfChange = true
                val newSel = when {
                    out.length > raw.length && out.length == 3 && raw.length == 2 -> 3
                    prev.length > raw.length && prevSel == 3 && raw.length == 2 -> 1
                    else -> out.length
                }
                editText.setText(out)
                editText.setSelection(newSel.coerceAtMost(out.length))
                selfChange = false
            }
        })
    }

    private fun normalizeTime(input: String): String {
        val digits = input.replace("[^\\d]".toRegex(), "")
        if (digits.isEmpty()) return ""
        val hh = when {
            digits.length >= 2 -> digits.substring(0, 2).toInt()
            else -> digits.substring(0, 1).toInt()
        }.coerceIn(0, 23)
        val mm = when {
            digits.length >= 4 -> digits.substring(2, 4).toInt()
            digits.length == 3 -> digits.substring(2, 3).toInt()
            else -> 0
        }.coerceIn(0, 59)
        return String.format("%02d:%02d", hh, mm)
    }

    private fun setupMultiSelect(textView: TextView, items: List<String>, selectedItems: MutableSet<String>) {
        textView.setOnClickListener {
            val checked = BooleanArray(items.size) { selectedItems.contains(items[it]) }
            MaterialAlertDialogBuilder(this)
                .setTitle("Válassz elemeket")
                .setMultiChoiceItems(items.toTypedArray(), checked) { _, which, isChecked ->
                    if (isChecked) selectedItems.add(items[which]) else selectedItems.remove(items[which])
                }
                .setPositiveButton("OK") { _, _ ->
                    textView.text = if (selectedItems.isNotEmpty()) selectedItems.joinToString(", ") else "Válassz..."
                }
                .setNegativeButton("Mégse", null)
                .show()
        }
    }

    private fun updateLocationsForLine(line: String) {
        val filteredLocations = routes.filter { it.line.trim() == line }
            .map { it.location.trim() }
            .distinct()
            .sorted()
        val locationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filteredLocations)
        binding.autoStartLocation.setAdapter(locationAdapter)
        binding.autoEndLocation.setAdapter(locationAdapter)
        binding.autoStartLocation.text.clear()
        binding.autoEndLocation.text.clear()
    }

    private fun loadData() {
        val storage = DataStorage(this)
        drivers.clear()
        drivers.addAll(storage.loadDrivers())
        routes.clear()
        routes.addAll(storage.loadRoutes())
        inspectors.clear()
        inspectors.addAll(storage.loadInspectors())
    }

    private fun createPdfWithTwoColumns(
        header: String,
        bodyText: String,
        positives: List<String>,
        negatives: List<String>,
        notes: String,
        driverCode: String
    ): File? {
        val doc = PdfDocument()

        // A4 portrait @72dpi
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f

        }

        // --- Margók, oszlopok, sortávok ---
        val marginX = 40f
        val lineStep = 22f
        val colLeftX = marginX               // bal hasáb x
        val colRightX = marginX + 250f       // jobb hasáb x (igény szerint módosítható)
        val contentMaxWidth = pageWidth - 2 * marginX

        // --- FEJLÉC KÉP (fejlec.png) ---
        var yPos = 0f // felső induló y
        try {
            val original = BitmapFactory.decodeResource(resources, R.drawable.fejlecpdf)
            if (original != null) {
                // Célszélesség: teljes tartalomszélesség
                val targetW = contentMaxWidth.toInt()
                // Aránytartó magasság
                val scale = targetW.toFloat() / original.width.toFloat()
                val targetH = (original.height * scale).toInt().coerceAtMost((pageHeight * 0.25f).toInt()) // max ~25% lapmagasság

                val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)

                // Középre igazítva a tartományon belül
                val imgX = marginX + (contentMaxWidth - scaled.width) / 2f
                canvas.drawBitmap(scaled, imgX, yPos, null)

                yPos += scaled.height + 18f  // kis térköz a kép után

                // Memóriaszivárgás elkerülése
                if (scaled != original) scaled.recycle()
                original.recycle()
            }
        } catch (_: Exception) {
            // Ha nincs kép, egyszerűen kihagyjuk – a PDF akkor is elkészül
        }

        // --- FEJLÉC SZÖVEG ---
        // Ha több soros header jöhet, tördeljük kényelmesen:
        val headerLines = header.split("\n")
        for (line in headerLines) {
            canvas.drawText(line, marginX, yPos, paint)
            yPos += lineStep + 6f
        }
        yPos += 6f

        // --- TÖRZSSZÖVEG (body) – egyszerű soronkénti kiírás ---
        val bodyLines = bodyText.split("\n")
        for (line in bodyLines) {
            canvas.drawText(line, marginX, yPos, paint)
            yPos += lineStep
        }

        yPos += 16f

        // --- CÍMSOROK A KÉT HASÁBHOZ ---
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("Pozitív észrevételek", colLeftX, yPos, paint)
        canvas.drawText("Negatív észrevételek", colRightX, yPos, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        yPos += lineStep

        // --- Két hasábba tördelés ---
        val maxCount = maxOf(positives.size, negatives.size)
        for (i in 0 until maxCount) {
            positives.getOrNull(i)?.let {
                paint.color = android.graphics.Color.parseColor("#009600") // sötétzöld
                    canvas.drawText("- $it", colLeftX, yPos, paint)
            }

            // --- Negatívak: piros ---
            negatives.getOrNull(i)?.let {
                paint.color = android.graphics.Color.parseColor("#B40000") // sötétpiros, ne égjen retina :D
                canvas.drawText("- $it", colRightX, yPos, paint)
            }
            yPos += lineStep
        }
        paint.setARGB(255, 0, 0, 0)

        yPos += 18f

        // --- MEGJEGYZÉS BLOKK ---
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("Megjegyzés:", marginX, yPos, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        yPos += lineStep

        // Ha a megjegyzés hosszú, egyszerű tördelés (durva wrap) a tartalomszélességre
        val noteWrapped = wrapText(notes, paint, contentMaxWidth)
        for (ln in noteWrapped) {
            if (yPos > pageHeight - marginX) break // primitív lapvédelem, (2. oldalhoz külön logika kellene)
            canvas.drawText(ln, marginX, yPos, paint)
            yPos += lineStep
        }

        // --- ZÁRÁS ÉS MENTÉS ---
        doc.finishPage(page)

        val outDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Ellenőrzések").apply { mkdirs() }
        val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date()) + ".pdf"
        val outFile = File(outDir, fileName)

        return try {
            FileOutputStream(outFile).use { doc.writeTo(it) }
            doc.close()
            outFile
        } catch (e: Exception) {
            doc.close()
            null
        }
    }

    /**
     * Egyszerű szövegtördelő: a Paint mérése alapján a megadott szélességre tördel.
     */
    private fun wrapText(text: String, paint: Paint, maxWidthPx: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.trim().split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        val sb = StringBuilder()

        for (w in words) {
            val probe = if (sb.isEmpty()) w else sb.toString() + " " + w
            if (paint.measureText(probe) <= maxWidthPx) {
                if (sb.isEmpty()) sb.append(w) else sb.append(' ').append(w)
            } else {
                if (sb.isNotEmpty()) lines.add(sb.toString())
                sb.clear()
                if (paint.measureText(w) <= maxWidthPx) {
                    sb.append(w)
                } else {
                    // nagyon hosszú "szó" (pl. kód) – vágjuk darabokra
                    var rest = w
                    while (rest.isNotEmpty()) {
                        var cut = rest.length
                        while (cut > 1 && paint.measureText(rest.substring(0, cut)) > maxWidthPx) cut--
                        lines.add(rest.substring(0, cut))
                        rest = rest.substring(cut)
                    }
                }
            }
        }
        if (sb.isNotEmpty()) lines.add(sb.toString())
        return lines
    }


data class Driver(val name: String, val code: String)
data class Route(val line: String, val location: String)
data class Inspector(val name: String, val code: String)
}

