package ru.efreet.trading.utils

import java.util.*


class SortedProperties : Properties() {

    override fun keys(): Enumeration<Any> {
        val keysEnum = super.keys()
        val keyList = Vector<Any>()

        while (keysEnum.hasMoreElements()) {
            keyList.add(keysEnum.nextElement())
        }

        Collections.sort(keyList, Comparator<Any> { o1, o2 -> o1.toString().compareTo(o2.toString()) })

        return keyList.elements()
    }

    companion object {
        private val serialVersionUID = 1L
    }
}