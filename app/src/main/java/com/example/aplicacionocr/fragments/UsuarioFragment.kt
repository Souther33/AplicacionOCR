package com.example.aplicacionocr.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.RESULT_CANCELED
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.aplicacionocr.R
import com.example.aplicacionocr.databinding.FragmentUsuarioBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class UsuarioFragment : Fragment() {

    private lateinit var binding: FragmentUsuarioBinding
    private lateinit var auth: FirebaseAuth
    private val documentoViewModel: FragmentsViewModel by activityViewModels()

    private val responseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == RESULT_OK) {
            val datos = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                val cuenta = datos.getResult(ApiException::class.java)
                if(cuenta != null) {
                    val credenciales = GoogleAuthProvider.getCredential(cuenta.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credenciales)
                        .addOnCompleteListener {
                            if(it.isSuccessful) {
                                mostrarUsuario()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireActivity(), it.message.toString(), Toast.LENGTH_SHORT).show()
                        }
                }
            } catch(e: ApiException) {
                Log.d("ERROR de API:>>>>", e.message.toString())
            }
        }
        if(it.resultCode == RESULT_CANCELED) {
            Toast.makeText(requireActivity(), "No se ha podido iniciar sesión, prueba más tarde.", Toast.LENGTH_SHORT).show()
        }
    }

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
            if(auth.currentUser == null) {
                login()
            } else {
                confirmarLogout()
            }
        }

        binding.btnBorrarDocumentosAll.setOnClickListener {
            confirmarBorrarDocumentosAll()
        }

    }

    private fun mostrarUsuario() {
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
            binding.btnLogout.apply {
                text = "Cerrar Sesión"
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red1))
            }

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

    private fun login() {
        val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(requireActivity(), googleConf)

        googleClient.signOut() //Fundamental para que no haga login automatico si he cerrado sesion

        responseLauncher.launch(googleClient.signInIntent)
    }
}