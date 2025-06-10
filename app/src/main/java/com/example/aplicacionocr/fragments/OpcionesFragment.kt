package com.example.aplicacionocr.fragments

import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.aplicacionocr.R
import com.example.aplicacionocr.databinding.FragmentOpcionesBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class OpcionesFragment : Fragment() {

    private lateinit var binding: FragmentOpcionesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOpcionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("preferencias", android.content.Context.MODE_PRIVATE)
        binding.swDesactivarNtfs.isChecked = prefs.getBoolean("notificaciones_activadas", true)

        setListeners()
    }

    private fun setListeners() {
        binding.btnAyuda.setOnClickListener {
            mostrarVentana(getString(R.string.titulo_ayuda), getString(R.string.mensaje_ayuda))
        }

        binding.btnSobreLaApp.setOnClickListener {
            mostrarVentana(getString(R.string.titulo_sobre_app), getString(R.string.mensaje_sobre_la_app))
        }

        binding.btnTerminos.setOnClickListener {
            mostrarVentana(getString(R.string.titulo_terminos), getString(R.string.mensaje_terminos))
        }

        binding.btnSoporte.setOnClickListener {
            mostrarVentana(getString(R.string.titulo_soporte), getString(R.string.mensaje_soporte))
        }

        binding.swDesactivarNtfs.setOnCheckedChangeListener { _, isChecked ->
            val prefs = requireContext().getSharedPreferences("preferencias", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("notificaciones_activadas", isChecked).apply()
        }
    }

    private fun mostrarVentana(titulo: String, mensaje: String) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.opciones, null)

        val textoTitulo = view.findViewById<TextView>(R.id.tv_opciones_titulo)
        textoTitulo.text = titulo

        val textoMensaje = view.findViewById<TextView>(R.id.tv_opciones_texto)
        textoMensaje.text = mensaje

        val btnCerrar = view.findViewById<ImageView>(R.id.ib_close)
        btnCerrar.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()

    }
}