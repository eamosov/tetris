package ru.efreet.trading.utils

import com.google.common.io.Files
import com.google.gson.GsonBuilder
import ru.efreet.trading.exchange.Instrument
import java.io.File
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties

/**
 * Created by fluder on 20/02/2018.
 */

private val rnd = Random()

private val student = arrayListOf(12.706, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228, 2.201, 2.179, 2.160, 2.145, 2.131, 2.120, 2.110, 2.101, 2.093, 2.086, 2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052, 2.048, 2.045, 2.042)


fun rnd(start: Int, end: Int): Int = if (start == end) start else start + rnd.nextInt(end - start + 1)

fun rnd(start: Double, end: Double): Double = if (start == end) start else start + rnd.nextDouble() * (end - start)

fun rnd(start: Float, end: Float): Float = if (start == end) start else start + rnd.nextFloat() * (end - start)

fun <T : Any> KClass<T>.getPropertyByName(name: String): KMutableProperty1<T, *> =
        this.memberProperties.first { it.name == name } as KMutableProperty1<T, *>

@Suppress("UNCHECKED_CAST")
fun <T : Any> String.parseNumberOrBool(cls: KClass<T>): T = when {
    cls.isSubclassOf(Int::class) -> this.toInt() as T
    cls.isSubclassOf(Float::class) -> this.toFloat() as T
    cls.isSubclassOf(Double::class) -> this.toDouble() as T
    cls.isSubclassOf(Boolean::class) -> this.toBoolean() as T
    else -> throw RuntimeException("coudn't convert String to ${cls.java.canonicalName}")
}

inline fun <reified T : Any> String.parseNumberOrBool(): T = parseNumberOrBool(T::class)

fun BigDecimal.round5(): BigDecimal = this.setScale(5, BigDecimal.ROUND_FLOOR)
fun BigDecimal.round4(): BigDecimal = this.setScale(4, BigDecimal.ROUND_FLOOR)

fun BigDecimal(value: Float): BigDecimal = BigDecimal(value.toString())

fun Double.pow2(): Double = this * this

fun Double.round2(): Double = (this * 100).toLong() / 100.0

fun Float.pow2(): Float = this * this

fun Float.round2(): Float = (this * 100F).toLong() / 100.0F

fun Float.round5(): Float = (this * 100000F).toLong() / 100000.0F


val gson = GsonBuilder().registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeType())
        .registerTypeAdapter(Instrument::class.java, InstrumentType())
        .setPrettyPrinting()
        .create()

inline fun <reified T> loadFromJson(path: String): T = gson.fromJson(Files.toString(File(path), Charsets.UTF_8), T::class.java)

fun Any.storeAsJson(path: String) {
    Files.write(toJson(), File(path), Charsets.UTF_8)
}

fun Any.toJson(): String = gson.toJson(this)

fun List<Pair<ZonedDateTime, Float>>.sma(timeFrame: Int): List<Pair<ZonedDateTime, Float>> {
    val out = mutableListOf<Pair<ZonedDateTime, Float>>()

    val sums = FloatArray(this.size, { 0.0F })
    for (index in 0 until this.size) {
        sums[index] = this[index].second
        if (index > 0) {
            sums[index] += sums[index - 1]
        }
        if (index >= timeFrame) {
            sums[index] -= this[index - timeFrame].second
        }
        val realTimeFrame = Math.min(timeFrame, index + 1)
        val value = sums[index] / realTimeFrame
        out.add(Pair(this[index].first, value))
    }

    return out
}

fun List<Pair<ZonedDateTime, Double>>.ema(timeFrame: Int): List<Pair<ZonedDateTime, Double>> {
    val out = mutableListOf<Pair<ZonedDateTime, Double>>()
    out.add(this[0])

    for (i in 1 until this.size) {
        out.add(Pair(this[i].first, out[i - 1].second + (2.0 / (timeFrame + 1)) * (this[i].second - out[i - 1].second)))
    }

    return out
}

fun List<Pair<ZonedDateTime, Double>>.dema(timeFrame: Int): List<Pair<ZonedDateTime, Double>> {

    val out = mutableListOf<Pair<ZonedDateTime, Double>>()

    val ema = this.ema(timeFrame)
    val emaEma = ema.ema(timeFrame)

    for (i in 0 until this.size)
        out.add(Pair(this[i].first, ema[i].second * 2 - emaEma[i].second))

    return out
}

fun roundAmount(amount: Float, price: Float): Float {
    var k = 1.0F
    while ((1.0F / price) * k < 1.0F) {
        k = k * 10.0F
    }

    return (amount * k).toInt() / k
}

fun ZonedDateTime.trimToBar(): ZonedDateTime = this.withSecond(59).withNano(0)
