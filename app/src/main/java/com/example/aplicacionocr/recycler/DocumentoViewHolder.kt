package com.example.aplicacionocr.recycler

import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.aplicacionocr.databinding.DocumentoLayoutBinding
import com.squareup.picasso.Picasso

class DocumentoViewHolder(v: View): RecyclerView.ViewHolder(v) {
    val binding = DocumentoLayoutBinding.bind(v)
    fun render(
        c: DocumentoModel,
        borrarDocumento: (Int) -> Unit,
        mostrarDetallesDocumento: (DocumentoModel) -> Unit
    ) {
        binding.tvNombreDocumento.text = c.nombre
        //Picasso.get().load(Uri.parse(c.imagen)).into(binding.ivImagenDocumento)
        Glide.with(binding.ivImagenDocumento.context)
            .load(Uri.parse(c.imagen))
            .into(binding.ivImagenDocumento)

        binding.btnBorrarDocumento.setOnClickListener {
            borrarDocumento(adapterPosition)
        }

        binding.btnEditarDocumento.setOnClickListener {
            mostrarDetallesDocumento(c)
        }
    }
}