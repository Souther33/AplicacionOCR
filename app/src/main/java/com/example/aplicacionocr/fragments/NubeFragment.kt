package com.example.aplicacionocr.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.aplicacionocr.R
import com.example.aplicacionocr.databinding.FragmentNubeBinding
import com.example.aplicacionocr.databinding.FragmentUsuarioBinding
import com.example.aplicacionocr.recycler.DocumentoModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.File
import com.google.gson.Gson

import androidx.fragment.app.activityViewModels
import com.example.aplicacionocr.fragments.FragmentsViewModel
import com.example.aplicacionocr.providers.db.CrudDocumentos
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


class NubeFragment : Fragment() {

    private lateinit var binding: FragmentNubeBinding
    private lateinit var auth: FirebaseAuth
    private val documentoViewModel: FragmentsViewModel by activityViewModels()
    private lateinit var credential: GoogleAccountCredential
    val formato = SimpleDateFormat("dd / MM / yyyy - HH:mm:ss", Locale.getDefault())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNubeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        mostrarBoton()

        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        credential = GoogleAccountCredential.usingOAuth2(
            requireContext(), listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account?.account

        setListeners()
    }

    private fun setListeners() {
        binding.btnCopiaSeguridad.setOnClickListener {
            val documentos = documentoViewModel.documentos.value ?: emptyList()
            if (documentos.isEmpty()) {
                Toast.makeText(requireContext(), "No hay documentos para respaldar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val zipFile = crearZipConDocumentos(requireContext(), documentos)
            subirBackupAGoogleDrive(credential, zipFile)  // ya tienes este méto_do hecho

            binding.tvCopiaSeguridad.text = getString(R.string.copia_seguridad_fecha) + "\n" + formato.format(Date(System.currentTimeMillis()))
        }

        binding.btnRestaurarCopia.setOnClickListener {
            restaurarDesdeDrive()
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun mostrarBoton() {
        val usuario = auth.currentUser

        if (usuario == null) {
            binding.tvCopiaSeguridad.text = "No se ha iniciado sesión"
            binding.btnCopiaSeguridad.isEnabled = false
            //binding.btnCopiaSeguridad.setBackgroundColor(R.color.btn_copia_seguridad_desactivado)
            /*binding.btnLogout.apply {
                text = "Iniciar Sesión"
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue1))
            }*/
        } else {
            binding.tvCopiaSeguridad.text = "Última copia de seguridad realizada:"
            binding.btnCopiaSeguridad.isEnabled = true
            //binding.btnCopiaSeguridad.setBackgroundColor(R.color.btn_copia_seguridad_activado)
        }
    }

    private fun subirBackupAGoogleDrive(credential: GoogleAccountCredential, archivoLocal: java.io.File) {
        Thread {
            try {
                val driveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("AplicacionOCR").build()

                val metadata = com.google.api.services.drive.model.File().apply {
                    name = "documentos_backup.zip"
                    mimeType = "application/zip"
                }

                val contentStream = FileContent("application/json", archivoLocal)

                driveService.files().create(metadata, contentStream).apply {
                    fields = "id"
                }.execute()

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Copia de seguridad subida a Drive", Toast.LENGTH_SHORT).show()
                }

            } catch (e: com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException) {
                requireActivity().runOnUiThread {
                    startActivityForResult(e.intent, 4001)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Error al subir la copia: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }


    private fun exportarDocumentosComoJson(context: Context, documentos: List<DocumentoModel>): File {
        val json = Gson().toJson(documentos)
        val file = File(context.cacheDir, "documentos_backup.json")

        if (file.exists()) file.delete()

        file.writeText(json)
        return file
    }

    private fun restaurarDesdeDrive() {
        Thread {
            try {
                val driveService = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("AplicacionOCR").build()

                val result = driveService.files().list()
                    .setQ("name = 'documentos_backup.zip' and mimeType = 'application/zip'")
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute()

                val archivos = result.files
                if (archivos.isEmpty()) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "No se encontró ninguna copia ZIP en Drive", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                val fileId = archivos[0].id
                val zipFile = File(requireContext().cacheDir, "documentos_backup.zip")
                zipFile.outputStream().use { fileOut ->
                    driveService.files().get(fileId).executeMediaAndDownloadTo(fileOut)
                }

                val documentos = mutableListOf<DocumentoModel>()
                val buffer = ByteArray(1024)
                val extractedDir = File(requireContext().filesDir, "restaurado_temp")
                extractedDir.mkdirs()

                ZipInputStream(zipFile.inputStream()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(extractedDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                            if (entry.name.endsWith(".json")) {
                                val jsonText = outFile.readText()
                                documentos += Gson().fromJson(jsonText, Array<DocumentoModel>::class.java).toList()
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                CrudDocumentos().borrarTodo()
                documentos.forEach { doc ->
                    //doc.imagen = File(extractedDir, "imagenes/${File(Uri.parse(doc.imagen).path!!).name}").absolutePath
                    val pdfFile = File(extractedDir, "pdfs/${File(doc.contenido).name}")
                    doc.contenido = pdfFile.absolutePath

                    val thumbFile = File(requireContext().filesDir, "thumb_${System.currentTimeMillis()}.png")
                    val exitoMiniatura = generarMiniaturaDesdePDF(pdfFile, thumbFile)
                    doc.imagen = if (exitoMiniatura) {
                        FileProvider.getUriForFile(
                            requireContext(),
                            "com.example.aplicacionocr.FileProvider",
                            thumbFile
                        ).toString()
                    } else {
                        ""
                    }

                    doc.contenido = File(extractedDir, "pdfs/${File(doc.contenido).name}").absolutePath
                    CrudDocumentos().create(doc)
                }
                val lista = CrudDocumentos().read()
                documentoViewModel.setListaDocumentos(lista)

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Documentos restaurados correctamente", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Error al restaurar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 4001) {
            val documentos = documentoViewModel.documentos.value ?: return
            val backupFile = exportarDocumentosComoJson(requireContext(), documentos)
            subirBackupAGoogleDrive(credential, backupFile)
        }
    }

    private fun crearZipConDocumentos(context: Context, documentos: List<DocumentoModel>): File {
        val zipFile = File(context.cacheDir, "backup_completo.zip")
        if (zipFile.exists()) zipFile.delete()

        val json = Gson().toJson(documentos)
        val jsonFile = File(context.cacheDir, "documentos_backup.json")
        jsonFile.writeText(json)

        ZipOutputStream(zipFile.outputStream()).use { zos ->
            fun addFileToZip(file: File, entryName: String) {
                if (!file.exists()) return
                val buffer = ByteArray(1024)
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { input ->
                    var len: Int
                    while (input.read(buffer).also { len = it } > 0) {
                        zos.write(buffer, 0, len)
                    }
                }
                zos.closeEntry()
            }

            // Añadir el JSON
            addFileToZip(jsonFile, "documentos_backup.json")

            // Añadir PDFs e imágenes
            documentos.forEach { doc ->
                addFileToZip(File(doc.contenido), "pdfs/${File(doc.contenido).name}")
                addFileToZip(File(Uri.parse(doc.imagen).path ?: ""), "imagenes/${File(Uri.parse(doc.imagen).path!!).name}")
            }
        }

        return zipFile
    }

    fun generarMiniaturaDesdePDF(pdfFile: File, outputImageFile: File): Boolean {
        return try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)
            val page = renderer.openPage(0)

            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            outputImageFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            page.close()
            renderer.close()
            fileDescriptor.close()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}