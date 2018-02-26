package ru.efreet.trading.utils

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1

/**
 * Created by fluder on 21/02/2018.
 */

data class PropRef<T>(val cls: KClass<*>, var propRef: PropRef<T>? = null, var const: Any? = null, var value: String) {

    fun get(receiver: T): Any? {
        return propRef?.get(receiver) ?: const
    }
}

data class PropertyEditorFactory<T : Any>(val cls: KClass<T>,
                                          val allProps: MutableMap<String, PropRef<T>> = mutableMapOf(),
                                          val genes: MutableList<PropertyEditor<T, Any?>> = mutableListOf()) {


    inline fun <reified R : Any?> of(kprop: KMutableProperty1<T, R>, key: String, min: R, max: R, step: R, hardBounds: Boolean): PropertyEditor<T, R> {

        val minRef = PropRef<T>(R::class, value = min.toString())
        val maxRef = PropRef<T>(R::class, value = max.toString())
        val stepRef = PropRef<T>(R::class, value = step.toString())
        val hardBoundsRef = PropRef<T>(Boolean::class, value = hardBounds.toString())

        allProps["${key}.min"] = minRef
        allProps["${key}.max"] = maxRef
        allProps["${key}.step"] = stepRef
        allProps["${key}.hardBounds"] = hardBoundsRef

        val pe = PropertyEditor(R::class, key, kprop, minRef, maxRef, stepRef, hardBoundsRef, min, max, step, hardBounds)
        genes.add(pe as PropertyEditor<T, Any?>)
        return pe
    }

    fun setMinMax(obj: T, p: Double, hardBounds: Boolean) {
        for (gene in genes) {
            gene.setMinMax(obj, p, hardBounds)
        }
    }

    fun setMinMax(properties: Properties) {

        for ((name, ref) in allProps) {
            val value = properties.getProperty(name)
            if (value != null) {
                if (value.startsWith("$")) {
                    ref.propRef = allProps[value.substring(1)]
                    ref.const = null
                } else {
                    ref.propRef = null
                    ref.const = value.parseNumberOrBool(ref.cls)
                }

                ref.value = value
            }
//            else {
//                ref.propRef = null
//                ref.const = null
//
//            }
        }
    }

    fun getMinMax(): Properties {
        val properties = SortedProperties()
        for ((name, ref) in allProps) {
            properties.setProperty(name, ref.value)
        }
        return properties
    }

    fun toLogicParams(properties: Properties): T {
        val prop = cls.java.newInstance()

        for (gene in genes) {
            properties.getProperty(gene.key)?.let {
                gene.kprop.set(prop, it.parseNumberOrBool(gene.cls))
            }
        }
        return prop
    }

    fun random(): T {
        val prop = cls.java.newInstance()

        for (gene in genes) {
            gene.setRandom(prop)
        }

        return prop
    }


    fun fromLogicParams(value: T): Properties {
        val p = SortedProperties()
        for (gene in genes) {
            p.setProperty(gene.key, gene.kprop.get(value).toString())
        }
        return p
    }

    fun copy(orig: T): T {
        val prop = cls.java.newInstance()
        for (gene in genes) {
            gene.kprop.set(prop, gene.kprop.get(orig))
        }
        return prop
    }

    fun log(params: T): String {
        val sb = StringBuffer()
        for (gene in genes.sortedBy { it.key }) {
            sb.append("${gene.kprop.name}: value=${gene.kprop.get(params)} min=${gene.getMin(params)} max=${gene.getMax(params)} step=${gene.getStep(params)} hardBounds=${gene.getHardBounds(params)}\n")
        }
        return sb.toString()
    }

}