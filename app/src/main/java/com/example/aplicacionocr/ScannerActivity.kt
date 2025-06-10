package com.example.aplicacionocr

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.aplicacionocr.databinding.ActivityScannerBinding
import com.example.aplicacionocr.providers.db.CrudDocumentos
import com.example.aplicacionocr.recycler.DocumentoModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.itextpdf.html2pdf.HtmlConverter
import jp.wasabeef.richeditor.RichEditor
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.itextpdf.kernel.pdf.*
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.canvas.PdfCanvas

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.Context

import org.opencv.android.OpenCVLoader

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.android.Utils
import java.io.FileOutputStream

import android.graphics.BitmapFactory


class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private lateinit var imageUrl: Uri
    val formato = SimpleDateFormat("MM.dd.yyyy-HH.mm.ss", Locale.getDefault())
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    //private lateinit var webView: WebView
    private lateinit var pdfDocument: PdfDocument
    private var currentPageNumber = 0

    private var documentoActual: DocumentoModel? = null

    private var modoEdicion: Boolean = false

    // When using Latin script library
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private lateinit var mEditor: RichEditor
    //private lateinit var mPreview: TextView

    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        // Procesar la imagen capturada
        recognizeTextFromImage(imageUrl)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "Error al cargar OpenCV", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "OpenCV cargado correctamente", Toast.LENGTH_SHORT).show()
        }

        pdfDocument = PdfDocument()
        currentPageNumber = 0

        documentoActual = intent.getSerializableExtra("PERSONAJE") as? DocumentoModel
        modoEdicion = documentoActual != null

        if (documentoActual != null) {
            imageUrl = Uri.parse(documentoActual!!.imagen)
        } else {
            imageUrl = createImageUri()
            contract.launch(imageUrl)
        }


        binding.etNombreDocumento2.setText("Documento " + formato.format(Date(System.currentTimeMillis())))

        // Cargar el contenido guardado
        val documento = intent.getSerializableExtra("PERSONAJE") as? DocumentoModel
        documento?.let {
            //binding.editorDeTexto2.setHtml(it.contenido)

            val pdfFile = File(it.contenido)

            if (pdfFile.exists()) {
                mostrarPdfConLibreria(pdfFile)
            } else {
                Toast.makeText(this, "No se encontró el PDF prototipo", Toast.LENGTH_SHORT).show()
            }
        }

        documentoActual = intent.getSerializableExtra("PERSONAJE") as? DocumentoModel

        documentoActual?.let {
            binding.etNombreDocumento2.setText(it.nombre)
            val pdfFile = File(it.contenido)
            if (pdfFile.exists()) {
                mostrarPdfConLibreria(pdfFile)
            } else {
                Toast.makeText(this, "No se encontró el PDF", Toast.LENGTH_SHORT).show()
            }
            imageUrl = Uri.parse(it.imagen) // para no perder la imagen
        }

        // Para poder cargar la imagen
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                imageUri?.let {
                    // Insertar la imagen en el RichEditor
                    mEditor.insertImage(it.toString(), "Imagen insertada")
                }
            }
        }

        // Para las notificaciones
        crearCanalNotificacion()


        setListeners()
    }

    private fun setListeners() {
        binding.btnDescargarS.setOnClickListener {
            descargarPdf()
        }

        binding.btnGuardarS.setOnClickListener {
            guardarRegistro()
        }

        binding.fabAddPage.setOnClickListener {
            // Lanza de nuevo la cámara para tomar una nueva foto
            imageUrl = createImageUri()
            contract.launch(imageUrl)
        }

    }

    private fun createImageUri(): Uri {
        //val image = File(filesDir, "camera_photos.png")
        val imageName = "IMG_${System.currentTimeMillis()}.png"
        val image = File(filesDir, imageName)
        return FileProvider.getUriForFile(this, "com.example.aplicacionocr.FileProvider", image)
    }

    private fun recognizeTextFromImage(imageUri: Uri) {
        val imageBitmap = mejorarImagenParaOCR(this, imageUri)

        if (imageBitmap != null) {
            val image = InputImage.fromBitmap(imageBitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val resultText = visionText.text
                    val htmlConFormato = convertirVisionTextAHtml(visionText)

                    if (modoEdicion) {
                        val pdfFile = File(documentoActual!!.contenido)
                        añadirTextoAlPDFExistente(pdfFile, visionText)
                    } else {
                        crearPdfConFormato(visionText)
                    }

                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        } else {
            Toast.makeText(this, "No se pudo procesar la imagen", Toast.LENGTH_SHORT).show()
        }
    }



    private fun crearPdfDesdeTexto(texto: String) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 tamaño

        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val lines = texto.split("\n")
        var y = 50

        for (line in lines) {
            canvas.drawText(line, 40f, y.toFloat(), paint)
            y += 20
            if (y > 800) break // Para evitar que se pase de la página
        }

        pdfDocument.finishPage(page)

        /*val file = File(filesDir, "TextoReconocido.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        Toast.makeText(this, "PDF guardado en: ${file.absolutePath}", Toast.LENGTH_LONG).show()

        abrirPdf(file)*/

        val filename = "TextoReconocido.pdf"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val contentUri = MediaStore.Files.getContentUri("external")
        val uri: Uri? = contentResolver.insert(contentUri, contentValues)

        try {
            uri?.let {
                val outputStream: OutputStream? = contentResolver.openOutputStream(it)
                outputStream?.use { stream ->
                    pdfDocument.writeTo(stream)
                }
                Toast.makeText(this, "PDF guardado en Descargas", Toast.LENGTH_LONG).show()
                abrirPdf(it)
            } ?: run {
                Toast.makeText(this, "No se pudo guardar el PDF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar el PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun abrirPdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No hay aplicación para abrir PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun crearPdfConFormato(visionText: com.google.mlkit.vision.text.Text) {
        val paint = Paint().apply {
            textSize = 12f
            isAntiAlias = true
        }

        if (currentPageNumber == 0) {
            pdfDocument = PdfDocument()
        }

        currentPageNumber += 1

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val inputImage = InputImage.fromFilePath(this, imageUrl)
        val scale = minOf(
            pageInfo.pageWidth.toFloat() / inputImage.width,
            pageInfo.pageHeight.toFloat() / inputImage.height
        )

        var minLeft = Float.MAX_VALUE
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                line.boundingBox?.left?.let {
                    if (it < minLeft) minLeft = it.toFloat()
                }
            }
        }

        val offsetX = minLeft * scale

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                line.boundingBox?.let { box ->
                    val x = (box.left * scale) - offsetX + 40f
                    val y = box.top * scale + paint.textSize
                    canvas.drawText(line.text, x, y, paint)
                }
            }
        }

        pdfDocument.finishPage(page)

        // Mostrar el PDF actualizado en PDFView
        val pdfFile = File(cacheDir, "texto_reconocido.pdf")
        try {
            pdfFile.outputStream().use { stream ->
                pdfDocument.writeTo(stream)
            }

            mostrarPdfConLibreria(pdfFile)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al mostrar el PDF", Toast.LENGTH_SHORT).show()
        }
    }




    private fun guardarRegistro() {
        val nombre = binding.etNombreDocumento2.text.toString().trim()
        val tipo = "scanner"

        val pathFinal = if (modoEdicion) {
            File(documentoActual!!.contenido)  // Reutilizamos el PDF ya existente modificado
        } else {
            File(filesDir, "prototipo_${System.currentTimeMillis()}.pdf").also { file ->
                file.outputStream().use { stream ->
                    pdfDocument.writeTo(stream)
                }
            }
        }

        val contenido = pathFinal.absolutePath

        // Generar miniatura desde la primera página del PDF
        val miniaturaFile = File(filesDir, "thumb_${System.currentTimeMillis()}.png")
        val exitoMiniatura = generarMiniaturaDesdePDF(pathFinal, miniaturaFile)

        val imagen = if (exitoMiniatura) {
            miniaturaFile.toURI().toString()
        } else {
            imageUrl.toString()  // fallback
        }

        if (modoEdicion) {
            val documentoEditado = DocumentoModel(
                documentoActual!!.id,
                nombre,
                imagen,
                contenido,
                tipo
            )

            if (CrudDocumentos().update(documentoEditado)) {
                Toast.makeText(this, "Documento actualizado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al actualizar documento", Toast.LENGTH_SHORT).show()
            }

        } else {
            val documentoNuevo = DocumentoModel(
                System.currentTimeMillis(),
                nombre,
                imagen,
                contenido,
                tipo
            )

            if (CrudDocumentos().create(documentoNuevo) != -1L) {
                Toast.makeText(this, "Documento añadido correctamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al añadir documento", Toast.LENGTH_SHORT).show()
            }
        }
    }




    //----------------------------------------------------------------------------------------------

    private fun convertirVisionTextAHtml(visionText: com.google.mlkit.vision.text.Text): String {
        val htmlBuilder = StringBuilder()
        htmlBuilder.append("<div style=\"font-family:sans-serif; font-size:14px;\">")

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val left = line.boundingBox?.left ?: 0
                // Dividir entre un factor para que no quede tan separado
                val margenIzquierdo = left / 2  // Puedes ajustar este valor según necesidad

                val textoEscapado = line.text
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")

                htmlBuilder.append("<p style=\"margin-left:${margenIzquierdo}px; margin-bottom:4px;\">$textoEscapado</p>")
            }
        }

        htmlBuilder.append("</div>")
        return htmlBuilder.toString()
    }

    private fun crearPdfDesdeHtml(contenidoHtml: String) {
        try {
            val filename = "DocumentoDesdeEditor.pdf"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val contentUri = MediaStore.Files.getContentUri("external")
            val uri = contentResolver.insert(contentUri, contentValues)

            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    HtmlConverter.convertToPdf(contenidoHtml, outputStream)
                }

                Toast.makeText(this, "PDF creado desde editor y guardado", Toast.LENGTH_LONG).show()
                abrirPdf(it)
            } ?: run {
                Toast.makeText(this, "No se pudo guardar el PDF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al crear el PDF", Toast.LENGTH_SHORT).show()
        }
    }

    /*private fun mostrarVentanaEmergente() {
        val popupView = layoutInflater.inflate(R.layout.popup_font_size, null)
        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        val spinner = popupView.findViewById<Spinner>(R.id.spinner_font_size)

        val tamanios = listOf("12", "14", "16", "18", "20", "24", "28", "32")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tamanios)
        spinner.adapter = adapter

        spinner.setSelection(tamanios.indexOf("14")) // Selección por defecto

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val size = tamanios[position].toInt()
                mEditor.setFontSize(size)
                popupWindow.dismiss()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        popupWindow.elevation = 10f
        popupWindow.showAsDropDown(binding.actionHeading1, 0, 20)
    }*/

    /*private fun mostrarPdfEnWebView(file: File) {
        val encodedPath = Uri.encode("file://${file.absolutePath}")
        val url = "file:///android_asset/pdfjs/web/viewer.html?file=$encodedPath"

        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.loadUrl(url)
    }*/

    private fun mostrarPdfConLibreria(file: File) {
        val pdfView = findViewById<com.github.barteksc.pdfviewer.PDFView>(R.id.pdfView)
        pdfView.fromFile(file)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .load()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            pdfDocument.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun añadirPaginaAlPDFExistente(pathPDFExistente: File, nuevaImagenUri: Uri) {
        try {
            val tempPdf = File(filesDir, "temp_${System.currentTimeMillis()}.pdf")
            val reader = PdfReader(pathPDFExistente)
            val writer = PdfWriter(tempPdf)
            val pdfDoc = PdfDocument(reader, writer)

            // Crea una nueva página al final del documento
            val newPage = pdfDoc.addNewPage()
            val canvas = com.itextpdf.kernel.pdf.canvas.PdfCanvas(newPage)

            val imageStream = contentResolver.openInputStream(nuevaImagenUri)
            val imageBytes = imageStream?.readBytes()
            imageStream?.close()

            if (imageBytes != null) {
                val imageData = ImageDataFactory.create(imageBytes)
                val image = Image(imageData)

                // Escala la imagen para que encaje en A4
                val pageWidth = newPage.pageSize.width
                val pageHeight = newPage.pageSize.height
                image.scaleToFit(pageWidth - 80, pageHeight - 80)
                image.setFixedPosition(40f, pageHeight - image.imageScaledHeight - 40f)

                val doc = Document(pdfDoc)
                doc.add(image)
                doc.close()  // Esto también cierra pdfDoc
            } else {
                pdfDoc.close()
            }

//-------------------------------------------------------------------------------
            //pathPDFExistente.delete()
            //tempPdf.renameTo(pathPDFExistente)

            val finalStream = pathPDFExistente.outputStream()
            tempPdf.inputStream().use { input ->
                finalStream.use { output ->
                    input.copyTo(output)
                }
            }
            tempPdf.delete()
            //------------------------------------------------------------------


            mostrarPdfConLibreria(pathPDFExistente)
            Toast.makeText(this, "Página añadida al PDF existente", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al añadir la nueva página", Toast.LENGTH_SHORT).show()
        }
    }

    private fun añadirTextoAlPDFExistente(pathPDFExistente: File, visionText: Text) {
        try {
            val tempPdf = File(filesDir, "temp_${System.currentTimeMillis()}.pdf")
            val reader = PdfReader(pathPDFExistente)
            val writer = PdfWriter(tempPdf)
            val pdfDoc = PdfDocument(reader, writer)

            // Crear nueva página al final
            val newPage = pdfDoc.addNewPage()
            val canvas = PdfCanvas(newPage)
            val font = com.itextpdf.kernel.font.PdfFontFactory.createFont()
            canvas.beginText()
            canvas.setFontAndSize(font, 12f)

            var y = newPage.pageSize.top - 40
            val leftMargin = 40f

            // 1. Encontrar el margen izquierdo mínimo
            var minLeft = Float.MAX_VALUE
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    line.boundingBox?.left?.let {
                        if (it < minLeft) minLeft = it.toFloat()
                    }
                }
            }

            val offsetX = minLeft

// 2. Escalar posiciones al tamaño de la página
            val inputImage = InputImage.fromFilePath(this, imageUrl)
            val scaleX = newPage.pageSize.width / inputImage.width.toFloat()
            val scaleY = newPage.pageSize.height / inputImage.height.toFloat()
            val scale = minOf(scaleX, scaleY)

            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    line.boundingBox?.let { box ->
                        val x = ((box.left.toFloat() - offsetX) * scale) + 40f
                        val yPosition = newPage.pageSize.top - ((box.top.toFloat()) * scale) - 20f
                        canvas.setTextMatrix(x, yPosition)
                        canvas.showText(line.text)
                    }
                }
            }


            canvas.endText()
            pdfDoc.close()

            pathPDFExistente.delete()
            tempPdf.renameTo(pathPDFExistente)

            mostrarPdfConLibreria(pathPDFExistente)
            Toast.makeText(this, "Texto añadido al PDF existente", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al añadir texto al PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun descargarPdf() {
        val origen = if (modoEdicion) {
            File(documentoActual!!.contenido)
        } else {
            val tempFile = File(cacheDir, "texto_reconocido.pdf")
            try {
                tempFile.outputStream().use { stream ->
                    pdfDocument.writeTo(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error al guardar PDF temporal", Toast.LENGTH_SHORT).show()
                return
            }
            tempFile
        }

        if (!origen.exists()) {
            Toast.makeText(this, "No hay PDF para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        val nombreArchivo = binding.etNombreDocumento2.text.toString().trim() + ".pdf"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val contentUri = MediaStore.Files.getContentUri("external")
        val uri = contentResolver.insert(contentUri, contentValues)

        try {
            uri?.let {
                contentResolver.openOutputStream(it)?.use { output ->
                    origen.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(this, "PDF guardado en Descargas", Toast.LENGTH_LONG).show()
                mostrarNotificacionPdfGuardado(nombreArchivo)
            } ?: run {
                Toast.makeText(this, "No se pudo guardar el PDF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarNotificacionPdfGuardado(nombreArchivo: String) {
        val prefs = getSharedPreferences("preferencias", MODE_PRIVATE)
        val notificacionesActivadas = prefs.getBoolean("notificaciones_activadas", true)

        if (!notificacionesActivadas) return

        try {
            val builder = NotificationCompat.Builder(this, "pdf_channel")
                .setSmallIcon(R.drawable.file_icon)
                .setContentTitle("PDF guardado")
                .setContentText("Se ha guardado $nombreArchivo en Descargas")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            with(NotificationManagerCompat.from(this)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "Permiso de notificación no concedido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "pdf_channel", // ID del canal
                "Notificaciones PDF", // Nombre visible
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Canal para notificaciones de PDFs guardados"
            }

            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
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

    fun mejorarImagenParaOCR(context: Context, uri: Uri): Bitmap? {
        return try {
            // Leer correctamente el bitmap desde el URI
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                Toast.makeText(context, "No se pudo decodificar la imagen", Toast.LENGTH_SHORT).show()
                return null
            }

            val mat = Mat()
            Utils.bitmapToMat(originalBitmap, mat)

            // Convertir a escala de grises
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)

            // Suavizado para reducir ruido
            Imgproc.GaussianBlur(mat, mat, Size(3.0, 3.0), 0.0)

            // Mejora del contraste
            Imgproc.equalizeHist(mat, mat)

            // Umbral adaptativo
            Imgproc.adaptiveThreshold(
                mat, mat, 255.0,
                Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY, 15, 10.0
            )

            // Rotar si es necesario
            if (mat.cols() > mat.rows()) {
                val rotated = Mat()
                Core.rotate(mat, rotated, Core.ROTATE_90_CLOCKWISE)
                mat.release()
                rotated.copyTo(mat)
            }

            // Convertir de nuevo a Bitmap
            val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(mat, resultBitmap)

            // Guardar para depuración
            val debugFile = File(context.filesDir, "debug_simple.png")
            FileOutputStream(debugFile).use {
                resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            Toast.makeText(context, "Imagen mejorada simple guardada", Toast.LENGTH_SHORT).show()

            resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al mejorar la imagen", Toast.LENGTH_SHORT).show()
            null
        }
    }



}