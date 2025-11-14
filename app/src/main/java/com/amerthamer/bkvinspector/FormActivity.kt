package com.amerthamer.bkvinspector

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
import androidx.core.content.FileProvider
import com.amerthamer.bkvinspector.data.DataStorage
import com.amerthamer.bkvinspector.databinding.ActivityFormBinding
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
    private var inspectionType: String = "Vonali"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toggleTypeGroup.check(R.id.btnVonali)
        binding.toggleTypeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            inspectionType = if (checkedId == R.id.btnMentori) "Mentori" else "Vonali"
        }

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
            updateLocationsForLine(allLines[position].trim())
        }

        binding.autoLine.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val typed = binding.autoLine.text.toString().trim()
                if (typed.isNotEmpty()) updateLocationsForLine(typed)
            }
        }

        setupTimeField(binding.editStartTime)
        setupTimeField(binding.editEndTime)

        setupMultiSelect(binding.txtPositive, positiveItems, selectedPositive)
        setupMultiSelect(binding.txtNegative, negativeItems, selectedNegative)

        binding.btnGeneratePdf.setOnClickListener {
            val inspectorRaw = binding.autoInspector.text.toString()
            val match = Regex("""^\s*(.*?)\s*\(([^)]+)\)\s*$""").find(inspectorRaw)
            val inspector = match?.groupValues?.get(1) ?: inspectorRaw
            val inspectorCode = match?.groupValues?.get(2) ?: ""

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

            val (header, targy) =
                if (inspectionType == "Vonali")
                    "Jelentés vonali ellenőrzésről" to "Vonali ellenőrzés"
                else
                    "Jelentés mentori ellenőrzésről" to "Mentori ellenőrzés"

            val bodyText = """
                Az ellenőrzés helye: $startLoc -> $endLoc
                Az ellenőrzés ideje: $date  $startTime ->$endTime
                Az ellenőrzést végezte: $inspector
                Az ellenőrzési igazolvány szám: $inspectorCode
                Az ellenőrzés tárgya: $targy
               
                Ellenőrzés adatai:

                Járművezető neve: $driverName ($driverCode) Viszonylat: $line Pályaszám: $vehicleCode
                Ellenőrzés kezdete: $startLoc $startTime
                Ellenőrzés vége: $endLoc $endTime

                Következő megállapításokat tettem:
            """.trimIndent()

            val pdfFile = createPdfWithTwoColumns(header, bodyText, positives, negatives, notes, driverCode)

            if (pdfFile != null) {
                val pdfUri = FileProvider.getUriForFile(this, "${packageName}.provider", pdfFile)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(pdfUri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivity(Intent.createChooser(intent, "PDF megnyitása"))
            }
        }
    }

    private fun setupTimeField(editText: android.widget.EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            var self = false
            var prev = ""
            var prevSel = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                prev = s?.toString() ?: ""
                prevSel = start + after
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (self) return
                val raw = s?.toString() ?: ""
                val d = raw.replace("[^\\d]".toRegex(), "")
                if (d.isEmpty()) return

                val lim = if (d.length > 4) d.substring(0, 4) else d
                val hStr = if (lim.length >= 2) lim.substring(0, 2) else lim
                val mStr = if (lim.length > 2) lim.substring(2) else ""

                var h = if (hStr.isNotEmpty()) hStr.toInt() else 0
                if (h > 23) h = 23

                var out = if (hStr.length >= 2) "%02d".format(h) else hStr
                if (out.length >= 2) {
                    out += ":"
                    if (mStr.isNotEmpty()) {
                        if (mStr.length >= 2) {
                            var m = mStr.substring(0, 2).toInt()
                            if (m > 59) m = 59
                            out += "%02d".format(m)
                        } else out += mStr
                    }
                }

                if (out == raw) return
                self = true
                val newSel =
                    if (out.length > raw.length && out.length == 3 && raw.length == 2) 3
                    else if (prev.length > raw.length && prevSel == 3 && raw.length == 2) 1
                    else out.length

                editText.setText(out)
                editText.setSelection(newSel.coerceAtMost(out.length))
                self = false
            }
        })
    }

    private fun normalizeTime(input: String): String {
        val d = input.replace("[^\\d]".toRegex(), "")
        if (d.isEmpty()) return ""
        val hh = (if (d.length >= 2) d.substring(0, 2) else d.substring(0, 1)).toInt().coerceIn(0, 23)
        val mm =
            if (d.length >= 4) d.substring(2, 4).toInt().coerceIn(0, 59)
            else if (d.length == 3) d.substring(2, 3).toInt().coerceIn(0, 59)
            else 0
        return "%02d:%02d".format(hh, mm)
    }

    private fun setupMultiSelect(textView: TextView, items: List<String>, selected: MutableSet<String>) {
        textView.setOnClickListener {
            val checked = BooleanArray(items.size) { selected.contains(items[it]) }
            MaterialAlertDialogBuilder(this)
                .setTitle("Válassz elemeket")
                .setMultiChoiceItems(items.toTypedArray(), checked) { _, which, isChecked ->
                    if (isChecked) selected.add(items[which]) else selected.remove(items[which])
                }
                .setPositiveButton("OK") { _, _ ->
                    textView.text = if (selected.isNotEmpty()) selected.joinToString(", ") else "Válassz..."
                }
                .setNegativeButton("Mégse", null)
                .show()
        }
    }

    private fun updateLocationsForLine(line: String) {
        val filtered = routes.filter { it.line.trim() == line }.map { it.location.trim() }.distinct().sorted()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filtered)
        binding.autoStartLocation.setAdapter(adapter)
        binding.autoEndLocation.setAdapter(adapter)
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
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f }

        val marginX = 40f
        val lineStep = 22f
        val colLeftX = marginX
        val colRightX = marginX + 250f
        val contentMaxWidth = pageWidth - 2 * marginX

        var yPos = 0f

        try {
            val original = BitmapFactory.decodeResource(resources, R.drawable.fejlecpdf)
            if (original != null) {
                val targetW = contentMaxWidth.toInt()
                val scale = targetW.toFloat() / original.width.toFloat()
                val targetH = (original.height * scale).toInt().coerceAtMost((pageHeight * 0.25f).toInt())
                val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)

                val imgX = marginX + (contentMaxWidth - scaled.width) / 2f
                canvas.drawBitmap(scaled, imgX, yPos, null)

                yPos += scaled.height + 18f
                if (scaled != original) scaled.recycle()
                original.recycle()
            }
        } catch (_: Exception) {}

        yPos += 12f

        val headerPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(header, pageWidth / 2f, 80f, headerPaint)
        yPos += 30f

        bodyText.split("\n").forEach {
            canvas.drawText(it, marginX, yPos, paint)
            yPos += lineStep
        }

        yPos += 16f

        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("Pozitív észrevételek", colLeftX, yPos, paint)
        canvas.drawText("Negatív észrevételek", colRightX, yPos, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        yPos += lineStep

        val max = maxOf(positives.size, negatives.size)
        for (i in 0 until max) {
            positives.getOrNull(i)?.let {
                paint.color = android.graphics.Color.parseColor("#009600")
                canvas.drawText("- $it", colLeftX, yPos, paint)
            }
            negatives.getOrNull(i)?.let {
                paint.color = android.graphics.Color.parseColor("#B40000")
                canvas.drawText("- $it", colRightX, yPos, paint)
            }
            yPos += lineStep
        }

        paint.setARGB(255, 0, 0, 0)
        yPos += 18f

        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        canvas.drawText("Megjegyzés:", marginX, yPos, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        yPos += lineStep

        val wrapped = wrapText(notes, paint, contentMaxWidth)
        for (ln in wrapped) {
            if (yPos > pageHeight - marginX) break
            canvas.drawText(ln, marginX, yPos, paint)
            yPos += lineStep
        }

        val closingPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }

        val now = SimpleDateFormat("yyyy.MM.dd. HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Az ellenőrzési jegyzőkönyvet lezártam: $now", 40f, 780f, closingPaint)

        doc.finishPage(page)

        val outDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Ellenőrzések").apply { mkdirs() }
        val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".pdf"
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
}
