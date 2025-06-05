package com.example.aplicacionocr.recycler

import android.net.Uri
import java.io.Serializable

data class DocumentoModel(
    val id: Long = System.currentTimeMillis(),
    val nombre: String,
    val imagen: String,
    val contenido: String
): Serializable
