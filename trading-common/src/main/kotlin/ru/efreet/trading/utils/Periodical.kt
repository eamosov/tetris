package ru.efreet.trading.utils

import java.time.Duration
import java.time.ZonedDateTime

/**
 * Created by fluder on 10/02/2018.
 */
class Periodical(val duration: Duration) {
    private var last = ZonedDateTime.now()

    fun invoke( block: ()->Unit, force:Boolean = false):Unit {
        if (last.isBefore(ZonedDateTime.now().minus(duration)) || force){
            last = ZonedDateTime.now()
            try {
                block()
            }catch (e:Throwable){
                e.printStackTrace()
            }
        }
    }
}