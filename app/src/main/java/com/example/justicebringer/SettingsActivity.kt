package com.example.justicebringer

import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.justicebringer.data.AppPreferences
import com.example.justicebringer.data.DataStorage
import com.example.justicebringer.databinding.ActivitySettingsBinding
import com.example.justicebringer.utils.FileUtils
import com.google.android.material.snackbar.Snackbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: AppPreferences

    private val pickDriverFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                takePersistable(it)
                prefs.saveDriverFile(it.toString())
                binding.txtDrivers.text = "Járművezetők fájl: $it"
            }
        }

    private val pickLocationFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                takePersistable(it)
                prefs.saveLocationFile(it.toString())
                binding.txtLocations.text = "Helyszínek fájl: $it"
            }
        }

    private val pickInspectorFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                takePersistable(it)
                prefs.saveInspectorFile(it.toString())
                binding.txtInspectors.text = "Ellenőrzők fájl: $it"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences(this)

        binding.btnPickDrivers.setOnClickListener {
            pickDriverFile.launch(arrayOf("text/plain"))
        }

        binding.btnPickLocations.setOnClickListener {
            pickLocationFile.launch(arrayOf("text/plain"))
        }

        binding.btnPickInspectors.setOnClickListener {
            pickInspectorFile.launch(arrayOf("text/plain"))
        }

        binding.btnUpload.setOnClickListener {
            val drivers = mutableListOf<FormActivity.Driver>()
            val routes = mutableListOf<FormActivity.Route>()
            val inspectors = mutableListOf<FormActivity.Inspector>()

            // járművezetők
            prefs.getDriverFile()?.let { uriString ->
                val lines = FileUtils.readTextFile(this, Uri.parse(uriString))
                for (line in lines) {
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val name = parts[0].trim()
                        val code = parts.subList(1, parts.size).joinToString(",").trim()
                        drivers.add(FormActivity.Driver(name, code))
                    }
                }
            }

            // helyszínek
            prefs.getLocationFile()?.let { uriString ->
                val lines = FileUtils.readTextFile(this, Uri.parse(uriString))
                for (line in lines) {
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val lineNumber = parts[0].trim()
                        val location = parts.subList(1, parts.size).joinToString(",").trim()
                        routes.add(FormActivity.Route(lineNumber, location))
                    }
                }
            }

            // ellenőrzők
            prefs.getInspectorFile()?.let { uriString ->
                val lines = FileUtils.readTextFile(this, Uri.parse(uriString))
                for (line in lines) {
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val name = parts[0].trim()
                        val code = parts[1].trim()
                        inspectors.add(FormActivity.Inspector(name, code))
                    }
                }
            }

            // mentés JSON formátumban
            val storage = DataStorage(this)
            storage.saveDrivers(drivers)
            storage.saveRoutes(routes)
            storage.saveInspectors(inspectors)

            Snackbar.make(
                binding.root,
                "Feldolgozva: ${drivers.size} járművezető, ${routes.size} helyszín, ${inspectors.size} ellenőrző",
                Snackbar.LENGTH_SHORT
            ).show()

            if (drivers.isNotEmpty() && routes.isNotEmpty() && inspectors.isNotEmpty()) {
                startActivity(Intent(this, FormActivity::class.java))
            }
        }

        // előző kiválasztások megjelenítése
        binding.txtDrivers.text = prefs.getDriverFile() ?: "Nincs kiválasztva"
        binding.txtLocations.text = prefs.getLocationFile() ?: "Nincs kiválasztva"
        binding.txtInspectors.text = prefs.getInspectorFile() ?: "Nincs kiválasztva"
    }

    private fun takePersistable(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri, FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) { }
    }
}
