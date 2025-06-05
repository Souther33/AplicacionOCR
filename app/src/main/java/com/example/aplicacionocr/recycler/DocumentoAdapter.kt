package com.example.aplicacionocr.recycler

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.aplicacionocr.R

class DocumentoAdapter(
    var lista: MutableList<DocumentoModel>,
    private val borrarDocumento: (Int) -> Unit,
    private val detallesDocumento: (DocumentoModel) -> Unit
): RecyclerView.Adapter<DocumentoViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentoViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.documento_layout, parent, false)
        return DocumentoViewHolder(v)
    }

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(holder: DocumentoViewHolder, position: Int) {
        holder.render(lista[position], borrarDocumento, detallesDocumento)
    }

    fun actualizarLista(nuevaLista: MutableList<DocumentoModel>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}