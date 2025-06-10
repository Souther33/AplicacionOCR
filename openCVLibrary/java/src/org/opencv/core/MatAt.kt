package org.opencv.core

import org.opencv.core.Mat.*

import java.lang.RuntimeException

// Conversión de UByteArray a ByteArray y viceversa
fun UByteArray.asByteArray(): ByteArray = this.map { it.toByte() }.toByteArray()
fun ByteArray.toUByteArray(): UByteArray = this.map { it.toUByte() }.toUByteArray()

// Conversión de UShortArray a ShortArray y viceversa
fun UShortArray.asShortArray(): ShortArray = this.map { it.toShort() }.toShortArray()
fun ShortArray.toUShortArray(): UShortArray = this.map { it.toUShort() }.toUShortArray()

fun Mat.get(row: Int, col: Int, data: UByteArray) = this.get(row, col, data.asByteArray())
fun Mat.get(indices: IntArray, data: UByteArray) = this.get(indices, data.asByteArray())
fun Mat.put(row: Int, col: Int, data: UByteArray) = this.put(row, col, data.asByteArray())
fun Mat.put(indices: IntArray, data: UByteArray) = this.put(indices, data.asByteArray())

fun Mat.get(row: Int, col: Int, data: UShortArray) = this.get(row, col, data.asShortArray())
fun Mat.get(indices: IntArray, data: UShortArray) = this.get(indices, data.asShortArray())
fun Mat.put(row: Int, col: Int, data: UShortArray) = this.put(row, col, data.asShortArray())
fun Mat.put(indices: IntArray, data: UShortArray) = this.put(indices, data.asShortArray())

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Mat.at(row: Int, col: Int): Atable<T> = when (T::class) {
    Byte::class, Double::class, Float::class, Int::class, Short::class -> this.at(T::class.java, row, col)
    UByte::class -> AtableUByte(this, row, col) as Atable<T>
    UShort::class -> AtableUShort(this, row, col) as Atable<T>
    else -> throw RuntimeException("Unsupported class type")
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Mat.at(idx: IntArray): Atable<T> = when (T::class) {
    Byte::class, Double::class, Float::class, Int::class, Short::class -> this.at(T::class.java, idx)
    UByte::class -> AtableUByte(this, idx) as Atable<T>
    UShort::class -> AtableUShort(this, idx) as Atable<T>
    else -> throw RuntimeException("Unsupported class type")
}

class AtableUByte(val mat: Mat, val indices: IntArray): Atable<UByte> {
    constructor(mat: Mat, row: Int, col: Int) : this(mat, intArrayOf(row, col))

    override fun getV(): UByte {
        val tmp = ByteArray(1)
        mat.get(indices, tmp)
        return tmp[0].toUByte()
    }

    override fun setV(v: UByte) {
        val tmp = byteArrayOf(v.toByte())
        mat.put(indices, tmp)
    }

    override fun getV2c(): Tuple2<UByte> {
        val tmp = ByteArray(2)
        mat.get(indices, tmp)
        return Tuple2(tmp[0].toUByte(), tmp[1].toUByte())
    }

    override fun setV2c(v: Tuple2<UByte>) {
        val tmp = byteArrayOf(v._0.toByte(), v._1.toByte())
        mat.put(indices, tmp)
    }

    override fun getV3c(): Tuple3<UByte> {
        val tmp = ByteArray(3)
        mat.get(indices, tmp)
        return Tuple3(tmp[0].toUByte(), tmp[1].toUByte(), tmp[2].toUByte())
    }

    override fun setV3c(v: Tuple3<UByte>) {
        val tmp = byteArrayOf(v._0.toByte(), v._1.toByte(), v._2.toByte())
        mat.put(indices, tmp)
    }

    override fun getV4c(): Tuple4<UByte> {
        val tmp = ByteArray(4)
        mat.get(indices, tmp)
        return Tuple4(tmp[0].toUByte(), tmp[1].toUByte(), tmp[2].toUByte(), tmp[3].toUByte())
    }

    override fun setV4c(v: Tuple4<UByte>) {
        val tmp = byteArrayOf(v._0.toByte(), v._1.toByte(), v._2.toByte(), v._3.toByte())
        mat.put(indices, tmp)
    }
}

class AtableUShort(val mat: Mat, val indices: IntArray): Atable<UShort> {
    constructor(mat: Mat, row: Int, col: Int) : this(mat, intArrayOf(row, col))

    override fun getV(): UShort {
        val tmp = ShortArray(1)
        mat.get(indices, tmp)
        return tmp[0].toUShort()
    }

    override fun setV(v: UShort) {
        val tmp = shortArrayOf(v.toShort())
        mat.put(indices, tmp)
    }

    override fun getV2c(): Tuple2<UShort> {
        val tmp = ShortArray(2)
        mat.get(indices, tmp)
        return Tuple2(tmp[0].toUShort(), tmp[1].toUShort())
    }

    override fun setV2c(v: Tuple2<UShort>) {
        val tmp = shortArrayOf(v._0.toShort(), v._1.toShort())
        mat.put(indices, tmp)
    }

    override fun getV3c(): Tuple3<UShort> {
        val tmp = ShortArray(3)
        mat.get(indices, tmp)
        return Tuple3(tmp[0].toUShort(), tmp[1].toUShort(), tmp[2].toUShort())
    }

    override fun setV3c(v: Tuple3<UShort>) {
        val tmp = shortArrayOf(v._0.toShort(), v._1.toShort(), v._2.toShort())
        mat.put(indices, tmp)
    }

    override fun getV4c(): Tuple4<UShort> {
        val tmp = ShortArray(4)
        mat.get(indices, tmp)
        return Tuple4(tmp[0].toUShort(), tmp[1].toUShort(), tmp[2].toUShort(), tmp[3].toUShort())
    }

    override fun setV4c(v: Tuple4<UShort>) {
        val tmp = shortArrayOf(v._0.toShort(), v._1.toShort(), v._2.toShort(), v._3.toShort())
        mat.put(indices, tmp)
    }
}

// Helpers
operator fun <T> Tuple2<T>.component1(): T = this._0
operator fun <T> Tuple2<T>.component2(): T = this._1

operator fun <T> Tuple3<T>.component1(): T = this._0
operator fun <T> Tuple3<T>.component2(): T = this._1
operator fun <T> Tuple3<T>.component3(): T = this._2

operator fun <T> Tuple4<T>.component1(): T = this._0
operator fun <T> Tuple4<T>.component2(): T = this._1
operator fun <T> Tuple4<T>.component3(): T = this._2
operator fun <T> Tuple4<T>.component4(): T = this._3

fun <T> T2(_0: T, _1: T): Tuple2<T> = Tuple2(_0, _1)
fun <T> T3(_0: T, _1: T, _2: T): Tuple3<T> = Tuple3(_0, _1, _2)
fun <T> T4(_0: T, _1: T, _2: T, _3: T): Tuple4<T> = Tuple4(_0, _1, _2, _3)
