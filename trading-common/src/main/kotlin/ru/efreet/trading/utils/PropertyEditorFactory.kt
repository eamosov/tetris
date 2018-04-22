package ru.efreet.trading.utils

import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

/**
 * Created by fluder on 21/02/2018.
 */

data class PropRef<T>(val cls: KClass<*>, var propRef: PropRef<T>? = null, var const: Any? = null, var value: String) {

    fun get(receiver: T?): Any? {
        return receiver?.let { propRef?.get(it) } ?: const
    }
}

class PropertyEditorFactory<T : Any>(val cls: KClass<T>) {

    companion object {

        @JvmStatic
        fun <T : Any> of(cls: Class<T>): PropertyEditorFactory<T> {
            return PropertyEditorFactory<T>(cls.kotlin)
        }
    }

    val allProps: MutableMap<String, PropRef<T>> = mutableMapOf()
    val genes: MutableList<PropertyEditor<T, Any?>> = mutableListOf()
    val consts: MutableList<PropertyEditor<T, Any?>> = mutableListOf()


    inline fun <reified R : Any?> of(kprop: KMutableProperty1<T, R>, key: String, min: R, max: R, step: R, hardBounds: Boolean): PropertyEditor<T, R> {
        return of(R::class.java, kprop, key, min, max, step, hardBounds)
    }

    fun <R : Any?> of(propCls: Class<R>, propName: String, key: String, min: R, max: R, step: R, hardBounds: Boolean): PropertyEditor<T, R> {

        return of(propCls, cls.memberProperties.find { it.name == propName } as KMutableProperty1<T, R>, key, min, max, step, hardBounds)
    }

    fun <R : Any?> of(propCls: Class<R>, kprop: KMutableProperty1<T, R>, key: String, min: R, max: R, step: R, hardBounds: Boolean): PropertyEditor<T, R> {

        val minRef = PropRef<T>((propCls as Class<Any>).kotlin, value = min.toString())
        val maxRef = PropRef<T>(propCls.kotlin, value = max.toString())
        val stepRef = PropRef<T>(propCls.kotlin, value = step.toString())
        val hardBoundsRef = PropRef<T>(Boolean::class, value = hardBounds.toString())

        allProps["${key}.min"] = minRef
        allProps["${key}.max"] = maxRef
        allProps["${key}.step"] = stepRef
        allProps["${key}.hardBounds"] = hardBoundsRef

        val pe = PropertyEditor(propCls.kotlin, key, kprop, minRef, maxRef, stepRef, hardBoundsRef, min, max, step, hardBounds)
        genes.add(pe as PropertyEditor<T, Any?>)
        return pe
    }

    inline fun <reified R : Any?> of(kprop: KMutableProperty1<T, R>, key: String, value: R): PropertyEditor<T, R> {
        val ref = PropRef<T>(R::class, value = value.toString())
        allProps[key] = ref

        val pe = PropertyEditor(R::class, key, kprop, ref, ref, ref, ref, value, value, value, true)
        consts.add(pe as PropertyEditor<T, Any?>)
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

        for (gene in consts) {
            properties.getProperty(gene.key)?.let {
                gene.kprop.set(prop, it.parseNumberOrBool(gene.cls))
            }
        }

        return prop
    }

    fun random(orig: T, copy: (T) -> T): T {
        val prop = copy(orig)

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

    fun isInitialized(value: T): Boolean {
        return genes.none { it.kprop.get(value) == null }
    }

//    fun copy(orig: T): T {
//        val prop = cls.java.newInstance()
//        for (gene in genes) {
//            gene.kprop.set(prop, gene.kprop.get(orig))
//        }
//        return prop
//    }

    fun log(params: T?): String {
        val sb = StringBuffer()
        for (gene in genes.sortedBy { it.key }) {
            sb.append("${gene.kprop.name}: value=${params?.let { gene.kprop.get(it) }} min =${gene.getMin(params)} max =${gene.getMax(params)} step =${gene.getStep(params)} hardBounds =${gene.getHardBounds(params)}\n")
        }
        for (gene in consts.sortedBy { it.key }) {
            sb.append("${gene.kprop.name}: value=${params?.let { gene.kprop.get(it) }}\n")
        }

        return sb.toString()
    }

}