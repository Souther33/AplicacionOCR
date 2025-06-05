package com.example.aplicacionocr.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.aplicacionocr.R
import com.example.aplicacionocr.databinding.FragmentUsuarioBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class UsuarioFragment : Fragment() {

    private lateinit var binding: FragmentUsuarioBinding
    private lateinit var auth: FirebaseAuth
    private val documentoViewModel: FragmentsViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUsuarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        mostrarUsuario()
        mostrarDatos()

        setListeners()
    }

    private fun setListeners() {
        binding.btnLogout.setOnClickListener {
            confirmarLogout()
        }

        binding.btnBorrarDocumentosAll.setOnClickListener {
            confirmarBorrarDocumentosAll()
        }

    }

    private fun mostrarUsuario() {
        /*if(auth.currentUser == null) {
            binding.tvUsuarioLogueado.text = "No se ha iniciado sesión"
        } else {
            binding.tvUsuarioLogueado.text = "Usuario: " + auth.currentUser?.email.toString()
        }*/

        val usuario = auth.currentUser

        if (usuario == null) {
            binding.tvUsuarioLogueado.text = "No se ha iniciado sesión"
            binding.ivUsuarioFoto.visibility = View.INVISIBLE
            binding.btnLogout.apply {
                text = "Iniciar Sesión"
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue1))
            }
        } else {
            binding.tvUsuarioLogueado.text = usuario.email

            val photoUrl = usuario.photoUrl
            if (photoUrl != null) {
                binding.ivUsuarioFoto.visibility = View.VISIBLE

                Glide.with(this)
                    .load(photoUrl)
                    .transform(RoundedCorners(300))
                    .into(binding.ivUsuarioFoto)
            } else {
                binding.ivUsuarioFoto.visibility = View.INVISIBLE
            }
        }
    }

    private fun mostrarDatos() {
        documentoViewModel.documentos.observe(viewLifecycleOwner) { listaDocumentos ->
            val cantidad = listaDocumentos.size
            binding.tvNumDocumentos.text = "Documentos registrados: $cantidad"
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun confirmarLogout() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cerrar Sesión")
        builder.setMessage("¿Estás seguro que deseas cerrar sesión?")
        builder.setPositiveButton("Sí") { dialog, _ ->
            auth.signOut()
            mostrarUsuario()
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()

    }

    private fun confirmarBorrarDocumentosAll() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Borrar Documentos")
        builder.setMessage("¿Estás seguro que deseas borrar todos lo documentos?")
        builder.setPositiveButton("Sí") { dialog, _ ->
            documentoViewModel.borrarTodos()
            Toast.makeText(requireContext(), "Se han borrado todos los documentos.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()

    }
}