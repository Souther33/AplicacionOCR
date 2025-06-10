package com.example.aplicacionocr.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.aplicacionocr.recycler.DocumentoModel
import com.example.aplicacionocr.providers.db.CrudDocumentos

class FragmentsViewModel: ViewModel() {
    private val _documentos = MutableLiveData<MutableList<DocumentoModel>>()
    val documentos: LiveData<MutableList<DocumentoModel>> = _documentos

    init {
        cargarDocumentos()
    }

    fun cargarDocumentos() {
        val lista = CrudDocumentos().read()
        _documentos.value = lista.toMutableList()
    }

    fun agregarDocumento(documento: DocumentoModel) {
        val resultado = CrudDocumentos().create(documento)
        if (resultado != -1L) {
            val listaActual = _documentos.value?.toMutableList() ?: mutableListOf()
            listaActual.add(documento)
            _documentos.value = listaActual
        }
    }

    fun borrarDocumento(documento: DocumentoModel) {
        val listaActual = _documentos.value ?: mutableListOf()
        if (CrudDocumentos().borrar(documento.id)) {
            listaActual.remove(documento)
            _documentos.value = listaActual
        }
    }

    fun borrarTodos() {
        val listaActual = _documentos.value ?: mutableListOf()
        var allDeleted = true
        for (doc in listaActual.toList()) { // Evitar concurrent modification
            if (!CrudDocumentos().borrar(doc.id)) {
                allDeleted = false
            }
        }
        if (allDeleted) {
            _documentos.value = mutableListOf()
        } else {
            cargarDocumentos() // recarga para estado consistente
        }
    }

    fun setListaDocumentos(nuevaLista: List<DocumentoModel>) {
        CrudDocumentos().borrarTodo()

        // Insertar uno a uno
        nuevaLista.forEach {
            println("Insertando: ${it.nombre}")
            CrudDocumentos().create(it)
        }


        // Actualizamos el LiveData
        _documentos.postValue(nuevaLista.toMutableList())
    }
}