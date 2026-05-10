package com.pmdm.pmdm08_pruegagithub3

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.pmdm.pmdm08_pruegagithub3.databinding.ActivityHomeBinding

enum class ProviderType {
    BASIC
}
class HomeActivity : AppCompatActivity() {
    // 1. Declarar el binding
    private lateinit var binding: ActivityHomeBinding

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 2. Inflar el binding correcto
        binding = ActivityHomeBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
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
    }


}