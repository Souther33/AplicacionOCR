package com.example.aplicacionocr.providers.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.aplicacionocr.recycler.DocumentoModel
import com.example.aplicacionocr.Aplicacion

class CrudDocumentos {
    fun create(c: DocumentoModel): Long {
        val con = Aplicacion.llave.writableDatabase
        return try {
            con.insertWithOnConflict(
                Aplicacion.TABLA,
                null,
                c.toContentValues(),
                SQLiteDatabase.CONFLICT_IGNORE
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            -1L
        } finally {
            con.close()
        }
    }

    fun read():MutableList<DocumentoModel> {
        val lista = mutableListOf<DocumentoModel>()
        val con = Aplicacion.llave.readableDatabase
        try {
            val cursor = con.query(
                Aplicacion.TABLA,
                arrayOf("id", "nombre", "imagen", "contenido", "tipo"),
                null,
                null,
                null,
                null,
                null
            )
            while(cursor.moveToNext()) {
                val personaje = DocumentoModel(
                    cursor.getLong(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4)
                )
                lista.add(personaje)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            con.close()
        }
        return lista
    }

    public fun borrar(id: Long): Boolean {
        val con = Aplicacion.llave.writableDatabase
        val personajeBorrado = con.delete(Aplicacion.TABLA, "id=?", arrayOf(id.toString()))
        con.close()
        return personajeBorrado > 0
    }

    public fun update(c: DocumentoModel): Boolean {
        val con = Aplicacion.llave.writableDatabase
        val values = c.toContentValues()
        var filasAfectadas = 0
        val q = "select id from ${Aplicacion.TABLA} where nombre=? AND id <> ?"
        val cursor = con.rawQuery(q, arrayOf(c.nombre, c.id.toString()))
        val existeNombre = cursor.moveToFirst()
        cursor.close()
        if(!existeNombre) {
            filasAfectadas = con.update(Aplicacion.TABLA, values, "id=?", arrayOf(c.id.toString()))
        }
        con.close()
        return filasAfectadas > 0
    }

    public fun borrarTodo() {
        val con = Aplicacion.llave.writableDatabase
        con.execSQL("delete from ${Aplicacion.TABLA}")
        con.close()
    }

    private fun DocumentoModel.toContentValues(): ContentValues {
        return ContentValues().apply {
            put("id", id)
            put("nombre", nombre)
            put("imagen", imagen)
            put("contenido", contenido)
            put("tipo", tipo)
        }
    }
}