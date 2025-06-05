package com.example.aplicacionocr

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplicacionocr.databinding.ActivityPrincipalBinding
import com.example.aplicacionocr.fragments.DocumentosFragment
import com.example.aplicacionocr.fragments.FragmentsViewModel
import com.example.aplicacionocr.fragments.NubeFragment
import com.example.aplicacionocr.fragments.OpcionesFragment
import com.example.aplicacionocr.fragments.UsuarioFragment
import com.example.aplicacionocr.providers.db.CrudDocumentos
import com.example.aplicacionocr.recycler.DocumentoAdapter
import com.example.aplicacionocr.recycler.DocumentoModel

class PrincipalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrincipalBinding
    var lista = mutableListOf<DocumentoModel>()
    private lateinit var adapter: DocumentoAdapter
    //private val documentoViewModel: FragmentsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setListeners()
        cargarFragmentPorDefecto()
    }

    private fun setListeners() {
        binding.fabCamara.setOnClickListener {
            //startActivity(Intent(this, CamaraActivity::class.java))
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        binding.ibFile.setOnClickListener {
            cargarFragments(DocumentosFragment())
            resaltarBotonActivo(binding.ibFile)
        }
        binding.tvFile.setOnClickListener {
            cargarFragments(DocumentosFragment())
            resaltarBotonActivo(binding.ibFile)
        }

        binding.ibUser.setOnClickListener {
            cargarFragments(UsuarioFragment())
            resaltarBotonActivo(binding.ibUser)
        }
        binding.tvUser.setOnClickListener {
            cargarFragments(UsuarioFragment())
            resaltarBotonActivo(binding.ibUser)
        }

        binding.ibSettings.setOnClickListener {
            cargarFragments(OpcionesFragment())
            resaltarBotonActivo(binding.ibSettings)
        }
        binding.tvSettings.setOnClickListener {
            cargarFragments(OpcionesFragment())
            resaltarBotonActivo(binding.ibSettings)
        }

        binding.ibCloud.setOnClickListener {
            cargarFragments(NubeFragment())
            resaltarBotonActivo(binding.ibCloud)
        }
        binding.tvCloud.setOnClickListener {
            cargarFragments(NubeFragment())
            resaltarBotonActivo(binding.ibCloud)
        }
    }

    private fun update(c: DocumentoModel) {
        val i = Intent(this, CamaraActivity::class.java).apply {
            putExtra("PERSONAJE", c)
        }

        startActivity(i)
    }

    private fun borrarDocumento(position: Int) {
        val id = lista[position].id
        // Eliminamos de la lista
        lista.removeAt(position)
        // Eliminamos de la base de datos
        if(CrudDocumentos().borrar(id)) {
            adapter.notifyItemRemoved(position)
        } else {
            Toast.makeText(this, "No se eliminó ningún registro", Toast.LENGTH_SHORT).show()
        }
    }

    private fun traerRegistros() {
        lista = CrudDocumentos().read()
    }

    private fun filtrarLista(query: String) {
        val listaFiltrada = lista.filter {
            it.nombre.contains(query, ignoreCase = true)
        }

        adapter.actualizarLista(listaFiltrada.toMutableList())
    }

    private fun cargarFragments(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container_view, fragment)
        transaction.commit()
    }

    private fun cargarFragmentPorDefecto() {
        cargarFragments(DocumentosFragment())
        resaltarBotonActivo(binding.ibFile)
    }

    private fun resaltarBotonActivo(botonActivo: ImageButton) {
        val botones = listOf(binding.ibFile, binding.ibUser, binding.ibSettings, binding.ibCloud)

        for (boton in botones) {
            if (boton == botonActivo) {
                boton.setColorFilter(getColor(R.color.amber), android.graphics.PorterDuff.Mode.SRC_IN)
            } else {
                boton.clearColorFilter()
            }
        }
    }

}