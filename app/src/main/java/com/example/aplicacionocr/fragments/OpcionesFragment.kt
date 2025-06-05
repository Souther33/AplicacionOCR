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
    val mensajeAyuda = """
Bienvenido a la sección de Ayuda.

Con esta aplicación puedes escanear documentos utilizando la cámara de tu dispositivo. La app utiliza tecnología OCR gratuita proporcionada por ML Kit para extraer el texto de las imágenes.

Una vez que el texto ha sido reconocido, puedes editarlo si lo deseas y luego convertirlo a PDF. Estos PDF se pueden guardar localmente o subir a la nube si estás logueado con tu cuenta de Google.

En la pantalla principal puedes acceder a tus documentos guardados organizados en tarjetas (CardView) dentro de una lista.

Para cualquier duda adicional, consulta la sección de Soporte.
""".trimIndent()

    val mensajeSobreLaApp = """
Esta aplicación está diseñada para facilitar la digitalización de documentos impresos.

Permite capturar imágenes de documentos mediante la cámara y convertirlas en texto utilizando OCR con ML Kit, sin necesidad de conexión a internet.

El texto extraído se puede editar directamente desde la app, y luego guardar como archivo PDF. Todos los documentos pueden almacenarse localmente y están organizados en una interfaz intuitiva basada en tarjetas.

Además, si el usuario inicia sesión con su cuenta de Google, puede subir los archivos PDF a la nube para mantener una copia de seguridad.

Rápida, ligera y completamente gratuita.
""".trimIndent()
    val mensajeTerminos = """
Términos y Condiciones / Política de Privacidad

1. Esta aplicación no almacena ni comparte tus datos personales sin tu consentimiento.
2. El reconocimiento de texto se realiza localmente en tu dispositivo utilizando ML Kit, por lo que tus imágenes y textos no se envían a servidores externos.
3. Si decides iniciar sesión con tu cuenta de Google, solo se utilizará para acceder a Google Drive y guardar tus archivos PDF en tu cuenta personal.
4. Los archivos PDF y textos editados se almacenan localmente en tu dispositivo, y puedes eliminarlos cuando lo desees.
5. Al usar esta aplicación, aceptas estos términos y te comprometes a utilizarla con fines legales y personales.

La aplicación es completamente gratuita y no incluye anuncios ni rastreadores.
""".trimIndent()
    val mensajeSoporte = """
¿Necesitas ayuda?

Si tienes problemas para escanear, convertir a PDF, acceder a tus documentos o cualquier otra duda sobre el funcionamiento de la app, no dudes en contactarnos.

Puedes escribirnos a:
soporte@tudominio.com

Intentaremos responderte lo antes posible. También estamos abiertos a sugerencias para mejorar la app y añadir nuevas funciones que te sean útiles.

Gracias por usar nuestra aplicación.
""".trimIndent()


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

        setListeners()
    }

    private fun setListeners() {
        binding.btnAyuda.setOnClickListener {
            mostrarVentana("Ayuda", mensajeAyuda)
        }

        binding.btnSobreLaApp.setOnClickListener {
            mostrarVentana("Sobre la App", mensajeSobreLaApp)
        }

        binding.btnTerminos.setOnClickListener {
            mostrarVentana("Términos y condiciones / Política de privacidad", mensajeTerminos)
        }

        binding.btnSoporte.setOnClickListener {
            mostrarVentana("Soporte o contacto", mensajeSoporte)
        }
    }

    private fun mostrarVentana1() {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Confirmación")
            builder.setMessage("¿Estás seguro que deseas continuar?")
            builder.setPositiveButton("Sí") { dialog, _ ->
                // Acción si acepta
                dialog.dismiss()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()

    }

    private fun mostrarVentana2() {
        val popupView = layoutInflater.inflate(R.layout.fragment_nube, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)

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