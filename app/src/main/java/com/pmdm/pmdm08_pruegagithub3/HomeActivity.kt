package com.pmdm.pmdm08_pruegagithub3

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.pmdm.pmdm08_pruegagithub3.databinding.ActivityHomeBinding
import java.io.IOException
import android.provider.Settings
import androidx.core.app.ActivityCompat

enum class ProviderType {
    BASIC
}
class HomeActivity : AppCompatActivity() {
    // 1. Declarar el binding
    private lateinit var binding: ActivityHomeBinding

    private val db = FirebaseFirestore.getInstance()

    // para la ubicación
    private val PERMISSIONS_READ_FINE_LOCATION = 100
    private lateinit var locationManager: LocationManager
    private val MIN_TIEMPO_ENTRE_UPDATES: Long = (1000 * 30) // 30 segundos
    private val MIN_CAMBIO_DISTANCIA_PARA_UPDATES = 2f // 2 metros
    private var mLatitud = 0.0
    private var mLongitud = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bundle = intent.extras
        val email = bundle?.getString("email")
        val provider = bundle?.getString("provider")
        setup(email ?: "", provider ?: "")

        // Guardado de datos
        val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        prefs.apply()
    }

    private fun setup(email: String, provider: String) {

        binding.emailTextView.text = email
        binding.providerTextView.text = provider
        binding.ubicacionTextView.text = ""
        
        binding.logOutButton.setOnClickListener {
            // Borrado de datos
            val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

            FirebaseAuth.getInstance().signOut()
            finish()
        }

        binding.saveButton.setOnClickListener {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // Si falla el token, puedes decidir si guardar los datos sin token o mostrar error
                    println("Error al obtener el token: ${task.exception}")
                    return@OnCompleteListener
                }

                val token = task.result

                db.collection("users").document(email).set(
                    hashMapOf(
                        "provider" to provider,
                        "address" to binding.adressEditText.text.toString(),
                        "phone" to binding.phoneEditText.text.toString(),
                        "token" to token)                    )

            })
        }

        binding.getButton.setOnClickListener {
            db.collection("users").document(email).get().addOnSuccessListener {
                binding.adressEditText.setText(it.get("address") as String?)
                binding.phoneEditText.setText(it.get("phone") as String?)
            }
        }

        binding.deleteButton.setOnClickListener {
            db.collection("users").document(email).delete()
        }

        binding.locationButton.setOnClickListener {
            if (binding.adressEditText.text.toString().isNotEmpty()) {

                // Construimos el objeto Geocoder: clase que transforma direcciones textuales
                // a coordenadas de Lat, Long y a la inversa
                val geocoder = Geocoder(this)
                val direccion = binding.adressEditText.text.toString()

                try {
                    geocoder.getFromLocationName(direccion, 10) { direcciones ->
                        if (direcciones.isNotEmpty()) {
                            val address = direcciones[0]
                            runOnUiThread {
                                mLatitud = address.latitude
                                mLongitud = address.longitude
                                binding.ubicacionTextView.text = "$mLatitud, $mLongitud"
                            }
                        } else {
                            Toast.makeText(
                                baseContext,
                                "Imposible obtener dirección",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: IOException) { // si no se introduce nada, ni dirección ni coordenadas
                    Toast.makeText(baseContext, "Imposible obtener dirección", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(baseContext, "Introduzca una dirección válida", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.myLocationButton.setOnClickListener {
            if (checkPermissions()) {
                activarGPS()
            } else {
                requestPermissions()
            }
            // obtenemos el servicio de posicionamiento
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            //Si el GPS (Mi Ubicación) no está activado en dispositivo
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(this, "Error al recuperar el GPS. Active el GPS", Toast.LENGTH_LONG)
                    .show()
            } else {
                try {
                    val listener = LocationListener { location ->
                        mLatitud = location.latitude
                        mLongitud = location.longitude
                        binding.ubicacionTextView.text = "$mLatitud, $mLongitud"
                    }

                    //Elegimos el proveedor de ubicación que estimemos oportuno.
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIEMPO_ENTRE_UPDATES,
                        MIN_CAMBIO_DISTANCIA_PARA_UPDATES,
                        listener
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error Try", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

        binding.viewMapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java).apply{
                putExtra("latitud", mLatitud)
                putExtra("longitud", mLongitud)
            }
            startActivity(intent)
        }
    }

    private fun activarGPS() {
        if (checkPermissions()) {
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            //si no está activada MiUbicaion en el dispositivo
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // muestra un diálogo de alerta para poder activarlo
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Ubicación desactivada")
                builder.setMessage("Por favor, actívela a continuación")
                builder.setPositiveButton(
                    "OK"
                ) { dialogInterface, i ->
                    val intent =
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
                val alertDialog: Dialog = builder.create()
                alertDialog.setCanceledOnTouchOutside(false)
                alertDialog.show()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true
        } else {
            requestPermissions()
            return false
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSIONS_READ_FINE_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_READ_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
                    activarGPS()
                // consultarUbicacion()
                } else {
                    Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }
}