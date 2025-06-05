package com.example.aplicacionocr.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.example.aplicacionocr.CamaraActivity
import com.example.aplicacionocr.databinding.FragmentDocumentosBinding
import com.example.aplicacionocr.providers.db.CrudDocumentos
import com.example.aplicacionocr.recycler.DocumentoAdapter
import com.example.aplicacionocr.recycler.DocumentoModel
import androidx.fragment.app.activityViewModels

class DocumentosFragment : Fragment() {

    private lateinit var binding: FragmentDocumentosBinding
    var lista = mutableListOf<DocumentoModel>()
    private lateinit var adapter: DocumentoAdapter
    private val documentoViewModel: FragmentsViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDocumentosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecycler()
        setListeners()

        documentoViewModel.documentos.observe(viewLifecycleOwner) { listaDocumentos ->
            lista = listaDocumentos.toMutableList()
            adapter.actualizarLista(lista)
            mostrarIcono()
        }
    }

    private fun setListeners() {
        binding.etBuscador2.setOnEditorActionListener { v, actionId, event ->
            val query = binding.etBuscador2.text.toString().trim()
            filtrarLista(query)
            true
        }
    }

    private fun setRecycler() {
        val layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recDocumentos2.layoutManager = layoutManager

        /*traerRegistros()
        adapter = DocumentoAdapter(lista, {position -> borrarDocumento(position)}, {c -> update(c)})*/
        //lista = documentoViewModel.documentos.value ?: mutableListOf()
        /*adapter = DocumentoAdapter(lista, { position ->
            documentoViewModel.borrarDocumento(lista[position])
        }, { c -> update(c) })*/
        adapter = DocumentoAdapter(mutableListOf(), { position ->
            documentoViewModel.borrarDocumento(lista[position])
        }, { c -> update(c) })
        // Ya no necesitas traerRegistros ni borrarDocumento manualmente aquí,
        // el ViewModel se encarga

        binding.recDocumentos2.adapter = adapter
    }

    private fun traerRegistros() {
        lista = CrudDocumentos().read()
    }

    private fun update(c: DocumentoModel) {
        val i = Intent(requireContext(), CamaraActivity::class.java).apply {
            putExtra("PERSONAJE", c)
        }

        startActivity(i)
    }

    private fun borrarDocumento(position: Int) {
        val id = lista[position].id
        // Eliminamos de la lista
        lista.removeAt(position)
        // Eliminamos de la base de datos
        if(CrudDocumentos().borrar(id)) {
            adapter.notifyItemRemoved(position)
        } else {
            Toast.makeText(requireContext(), "No se eliminó ningún registro", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filtrarLista(query: String) {
        val listaFiltrada = lista.filter {
            it.nombre.contains(query, ignoreCase = true)
        }

        adapter.actualizarLista(listaFiltrada.toMutableList())
    }

    override fun onResume() {
        super.onResume()
        documentoViewModel.cargarDocumentos()
    }

    private fun mostrarIcono() {
        if(lista.size > 0) {
            binding.ivVacio.visibility = View.INVISIBLE
            binding.tvVacio.visibility = View.INVISIBLE
        } else {
            binding.ivVacio.visibility = View.VISIBLE
            binding.tvVacio.visibility = View.VISIBLE
        }
    }

}