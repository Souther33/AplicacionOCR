package com.example.aplicacionocr.recycler

import android.net.Uri
import java.io.Serializable

data class DocumentoModel(
    val id: Long = System.currentTimeMillis(),
    var nombre: String,
    var imagen: String,
    var contenido: String,
    var tipo: String
): Serializable
