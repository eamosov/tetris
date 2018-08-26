package ru.efreet.trading.utils

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.isSubclassOf

/**
 * Created by fluder on 21/02/2018.
 */
data class PropertyEditor<T, R : Any?>(val cls: KClass<*>,
                                       val key: String,
                                       val kprop: KMutableProperty1<T, R>,
                                       val propRefMin: PropRef<T>,
                                       val propRefMax: PropRef<T>,
                                       val propRefStep: PropRef<T>,
                                       val hardBoundsRef: PropRef<T>,
                                       val min: R,
                                       val max: R,
                                       val step: R,
                                       val hardBounds: Boolean) {


    @Suppress("UNCHECKED_CAST")
    fun getMin(obj: T?): R = (propRefMin.get(obj) ?: min) as R

    @Suppress("UNCHECKED_CAST")
    fun getMax(obj: T?): R = (propRefMax.get(obj) ?: max) as R

    @Suppress("UNCHECKED_CAST")
    fun getStep(obj: T?): R = (propRefStep.get(obj) ?: step) as R

    fun getHardBounds(obj: T?): Boolean = (hardBoundsRef.get(obj) ?: hardBounds) as Boolean

    fun getValue(obj: T): R = kprop.get(obj)

    fun setValue(obj: T, value: R): R {
        kprop.set(obj, value)
        return value
    }

    @Suppress("UNCHECKED_CAST")
    fun setRandom(obj: T): R = when {
        cls.isSubclassOf(Int::class) -> setValue(obj, rnd(getMin(obj) as Int, getMax(obj) as Int) as R)
        cls.isSubclassOf(Float::class) -> setValue(obj, rnd(getMin(obj) as Float, getMax(obj) as Float) as R)
        cls.isSubclassOf(Boolean::class) -> setValue(obj, (rnd(0, 1) != 0) as R)
        else -> throw RuntimeException("Couldn't get random of class ${cls}")
    }

    @Suppress("UNCHECKED_CAST")
    fun step(obj: T, steps: Int): R {
        var value = getValue(obj)
        if (value == null) {
            value = setRandom(obj)
            return value
        }

        val n: R

        val min = getMin(obj)
        val max = getMax(obj)
        val step = getStep(obj)

        if (cls.isSubclassOf(Int::class)) {
            var _n = (value as Int) + (step as Int) * steps
            if (getHardBounds(obj)) {
                if (_n < min as Int)
                    _n = min
                else if (_n > max as Int)
                    _n = max

            }else if (_n < 1){
                _n = 1
            }
            n = _n as R
        } else if (cls.isSubclassOf(Float::class)) {
            var _n = (value as Float) + (step as Float) * steps
            if (getHardBounds(obj)) {
                if (_n < min as Float)
                    _n = min
                else if (_n > max as Float)
                    _n = max

            }else if (_n < 0.0){
                _n = 0.0F
            }
            n = _n as R
        } else if (cls.isSubclassOf(Boolean::class)) {
            val _n = !(value as Boolean)
            n = _n as R
        } else {
            throw RuntimeException("Couldn't do step for class ${cls}")
        }

        setValue(obj, n)
        return n
    }

    @Suppress("UNCHECKED_CAST")
    fun step2(obj: T, stepSizeP: Float) {
        var value = getValue(obj)
        if (value == null) {
            value = setRandom(obj)
        }

        val n: R

        val min = getMin(obj)
        val max = getMax(obj)
        val step = getStep(obj)

        if (cls.isSubclassOf(Int::class)) {

            var stepSize = ((max as Int - min as Int) * stepSizeP).toInt()
            if (Math.abs(stepSize) < step as Int) {
                stepSize = (step as Int) * if (stepSize > 0) 1 else -1
            }

            var _n = (value as Int) + stepSize
            if (getHardBounds(obj)) {
                if (_n < min as Int)
                    _n = min
                else if (_n > max as Int)
                    _n = max

            }else if (_n < 1){
                _n = 1
            }
            n = _n as R
        } else if (cls.isSubclassOf(Float::class)) {

            var stepSize = (max as Float - min as Float) * stepSizeP
            if (Math.abs(stepSize) < step as Float) {
                stepSize = (step as Float) * if (stepSize > 0) 1 else -1
            }

            var _n = (value as Float) + stepSize
            if (getHardBounds(obj)) {
                if (_n < min as Float)
                    _n = min
                else if (_n > max as Float)
                    _n = max

            }else if (_n < 0.0F){
                _n = 0.0F
            }
            n = _n as R
        } else if (cls.isSubclassOf(Boolean::class)) {
            val _n = !(value as Boolean)
            n = _n as R
        } else {
            throw RuntimeException("Couldn't do step for class ${cls}")
        }

        setValue(obj, n)
    }

    fun setMinMax(obj: T, p: Float, hardBounds: Boolean) {

        if (cls.isSubclassOf(Int::class)) {
            propRefMin.const = Math.floor((kprop.get(obj) as Int).toFloat() * (1.0 - p / 100.0)).toInt()
            propRefMax.const = Math.ceil((kprop.get(obj) as Int).toFloat() * (1.0 + p / 100.0)).toInt()
        } else if (cls.isSubclassOf(Float::class)) {
            propRefMin.const = (kprop.get(obj) as Float) * (1.0 - p / 100.0)
            propRefMax.const = (kprop.get(obj) as Float) * (1.0 + p / 100.0)
        }
        hardBoundsRef.const = hardBounds
        hardBoundsRef.propRef = null
        hardBoundsRef.value = hardBoundsRef.const.toString()
        propRefMin.propRef = null
        propRefMin.value = propRefMin.const.toString()
        propRefMax.propRef = null
        propRefMax.value = propRefMax.const.toString()
    }
}