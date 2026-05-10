package com.pmdm.pmdm08_pruegagithub3

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
// IMPORTANTE: Asegúrate de usar ActivityHomeBinding
import com.pmdm.pmdm08_pruegagithub3.databinding.ActivityHomeBinding

enum class ProviderType {
    BASIC
}
class HomeActivity : AppCompatActivity() {
    
    // 1. Cambiado de ActivityAuthBinding a ActivityHomeBinding
    private lateinit var binding: ActivityHomeBinding

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
    }

    private fun setup(email: String, provider: String) {
        title = "Inicio"
        
        // 3. Ahora podrás acceder a los IDs de activity_home.xml
        binding.emailTextView.text = email
        binding.providerTextView.text = provider
        
        binding.logOutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            finish() // Es mejor usar finish() para volver atrás
        }
    }
}