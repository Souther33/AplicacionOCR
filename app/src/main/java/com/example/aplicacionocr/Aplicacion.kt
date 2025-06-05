package com.example.aplicacionocr

import android.app.Application
import android.content.Context
import com.example.aplicacionocr.providers.db.MyDatabase

class Aplicacion: Application() {
    companion object {
        const val VERSION = 1
        const val DB = "Base_de_datos_1"
        const val TABLA = "documentos"
        lateinit var contexto: Context
        lateinit var llave: MyDatabase
    }

    override fun onCreate() {
        super.onCreate()
        contexto = applicationContext
        llave = MyDatabase()
    }
}