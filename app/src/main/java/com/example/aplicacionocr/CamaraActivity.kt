package com.example.aplicacionocr

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.aplicacionocr.databinding.ActivityCamaraBinding
import com.example.aplicacionocr.providers.db.CrudDocumentos
import com.example.aplicacionocr.recycler.DocumentoModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.itextpdf.html2pdf.HtmlConverter
import jp.wasabeef.richeditor.RichEditor
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.Activity

class CamaraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCamaraBinding
    private lateinit var imageUrl: Uri
    val formato = SimpleDateFormat("MM.dd.yyyy-HH.mm.ss", Locale.getDefault())
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    // When using Latin script library
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /*// When using Chinese script library
    val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    // When using Devanagari script library
    val recognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

    // When using Japanese script library
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    // When using Korean script library
    val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())*/

    private lateinit var mEditor: RichEditor
    //private lateinit var mPreview: TextView

    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        //binding.captureImageView.setImageURI(null)
        //binding.captureImageView.setImageURI(imageUrl)

        //binding.editorDeTexto

        // Procesar la imagen capturada
        recognizeTextFromImage(imageUrl)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCamaraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mEditor = findViewById(R.id.editor_de_texto)

        imageUrl = createImageUri()
        contract.launch(imageUrl)
        binding.etNombreDocumento.setText("Documento " + formato.format(Date(System.currentTimeMillis())))

        // Cargar el contenido guardado
        val documento = intent.getSerializableExtra("PERSONAJE") as? DocumentoModel
        documento?.let {
            binding.editorDeTexto.setHtml(it.contenido)
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

        setListeners()
    }

    private fun setListeners() {
        binding.btnCamara.setOnClickListener {
            contract.launch(imageUrl)
        }

        binding.btnGuardarDocumento.setOnClickListener {
            //recognizeTextFromImage(imageUrl)

            /*val contenidoHtml = binding.editorDeTexto.html ?: ""
            val textoPlano = android.text.Html.fromHtml(contenidoHtml, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
            crearPdfDesdeTexto(textoPlano)*/

            val contenidoHtml = binding.editorDeTexto.html ?: ""
            crearPdfDesdeHtml(contenidoHtml)
        }

        binding.btnGuardarRegistro.setOnClickListener {
            guardarRegistro()
        }

        //------------------------------------------------------------------------------------------

        binding.actionUndo.setOnClickListener { mEditor.undo() }
        binding.actionRedo.setOnClickListener { mEditor.redo() }
        binding.actionBold.setOnClickListener { mEditor.setBold() }
        binding.actionItalic.setOnClickListener { mEditor.setItalic() }
        binding.actionSubscript.setOnClickListener { mEditor.setSubscript() }
        binding.actionSuperscript.setOnClickListener { mEditor.setSuperscript() }
        binding.actionStrikethrough.setOnClickListener { mEditor.setStrikeThrough() }
        binding.actionUnderline.setOnClickListener { mEditor.setUnderline() }
        binding.actionHeading1.setOnClickListener { mEditor.setHeading(1) }
        binding.actionHeading2.setOnClickListener { mEditor.setHeading(2) }
        binding.actionHeading3.setOnClickListener { mEditor.setHeading(3) }
        binding.actionHeading4.setOnClickListener { mEditor.setHeading(4) }
        binding.actionHeading5.setOnClickListener { mEditor.setHeading(5) }
        binding.actionHeading6.setOnClickListener { mEditor.setHeading(6) }

        var isTextColorChanged = false
        binding.actionTxtColor.setOnClickListener {
            mEditor.setTextColor(if (isTextColorChanged) Color.BLACK else Color.RED)
            isTextColorChanged = !isTextColorChanged
        }

        var isBgColorChanged = false
        binding.actionBgColor.setOnClickListener {
            mEditor.setTextBackgroundColor(if (isBgColorChanged) Color.TRANSPARENT else Color.YELLOW)
            isBgColorChanged = !isBgColorChanged
        }

        binding.actionIndent.setOnClickListener { mEditor.setIndent() }
        binding.actionOutdent.setOnClickListener { mEditor.setOutdent() }
        binding.actionAlignLeft.setOnClickListener { mEditor.setAlignLeft() }
        binding.actionAlignCenter.setOnClickListener { mEditor.setAlignCenter() }
        binding.actionAlignRight.setOnClickListener { mEditor.setAlignRight() }
        binding.actionBlockquote.setOnClickListener { mEditor.setBlockquote() }
        binding.actionInsertBullets.setOnClickListener { mEditor.setBullets() }
        binding.actionInsertNumbers.setOnClickListener { mEditor.setNumbers() }

        binding.actionInsertImage.setOnClickListener {
            /*mEditor.insertImage(
                "https://raw.githubusercontent.com/wasabeef/art/master/chip.jpg",
                "dachshund",
                320
            )*/

            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }

        binding.actionInsertLink.setOnClickListener {
            mEditor.insertLink("https://github.com/wasabeef", "wasabeef")
        }

        binding.actionInsertCheckbox.setOnClickListener {
            mEditor.insertTodo()
        }

    }

    private fun createImageUri(): Uri {
        //val image = File(filesDir, "camera_photos.png")
        val imageName = "IMG_${System.currentTimeMillis()}.png"
        val image = File(filesDir, imageName)
        return FileProvider.getUriForFile(this, "com.example.aplicacionocr.FileProvider", image)
    }

    private fun recognizeTextFromImage(imageUri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Aquí obtienes el texto reconocido
                    val resultText = visionText.text

                    /*//binding.textoReconocido.text = resultText // Muestra el texto en tu UI
                    binding.editorDeTexto.html = resultText.replace("\n", "<br>")*/

                    val htmlConFormato = convertirVisionTextAHtml(visionText)
                    binding.editorDeTexto.html = htmlConFormato


                    //crearPdfDesdeTexto(resultText)
                    crearPdfConFormato(visionText)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    binding.textoReconocido.text = "Error al reconocer texto"
                }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.textoReconocido.text = "Error al cargar la imagen"
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

    /*private fun abrirPdf(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "com.example.aplicacionocr.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        // Verifica si hay alguna app que pueda abrir el PDF
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No hay aplicación para abrir PDF", Toast.LENGTH_SHORT).show()
        }
    }*/

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
        val pdfDocument = PdfDocument()
        val paint = Paint()
        paint.textSize = 12f
        paint.isAntiAlias = true

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Tamaño A4 en puntos
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Toma el tamaño real de la imagen original
        val inputImage = InputImage.fromFilePath(this, imageUrl)
        val imageWidth = inputImage.width.toFloat()
        val imageHeight = inputImage.height.toFloat()
        val pdfWidth = pageInfo.pageWidth.toFloat()
        val pdfHeight = pageInfo.pageHeight.toFloat()

        // Calcula escala para ajustar al PDF respetando proporciones
        val scaleX = pdfWidth / imageWidth
        val scaleY = pdfHeight / imageHeight
        val scale = minOf(scaleX, scaleY) // para evitar desbordes

        // 1. Encuentra el valor mínimo de X (el borde izquierdo más a la izquierda)
        var minLeft = Float.MAX_VALUE
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                line.boundingBox?.let {
                    val left = it.left.toFloat()
                    if (left < minLeft) minLeft = left
                }
            }
        }

        // 2. Calcula el desplazamiento para quitar ese margen y dejarlo bien alineado
        val offsetX = minLeft * scale

        // 3. Dibuja ajustando el margen izquierdo
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                if (box != null) {
                    val x = (box.left * scale) - offsetX + 40f  // 40f de margen izquierdo en el PDF
                    val y = box.top * scale + paint.textSize
                    canvas.drawText(line.text, x, y, paint)
                }
            }
        }

        pdfDocument.finishPage(page)

        val filename = "TextoReconocidoFormato.pdf"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val contentUri = MediaStore.Files.getContentUri("external")
        val uri = contentResolver.insert(contentUri, contentValues)

        try {
            uri?.let {
                val outputStream = contentResolver.openOutputStream(it)
                outputStream?.use { stream ->
                    pdfDocument.writeTo(stream)
                }
                Toast.makeText(this, "PDF con formato guardado en Descargas", Toast.LENGTH_LONG).show()
                abrirPdf(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar el PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }


    private fun guardarRegistro() {
        val c = DocumentoModel(System.currentTimeMillis(), binding.etNombreDocumento.text.toString().trim(), imageUrl.toString(), binding.editorDeTexto.html ?: "")

        if(CrudDocumentos().create(c) != -1L) {
            Toast.makeText(this, "Se ha añadido el documento a los registros", Toast.LENGTH_SHORT).show()
            finish()
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

}