package ru.efreet.trading.exchange

import java.time.Duration
import java.time.temporal.Temporal
import java.time.temporal.TemporalUnit

/**
 * Created by fluder on 08/02/2018.
 */
enum class BarInterval(private val _duration: Duration) : TemporalUnit {

    ONE_SECOND(Duration.ofSeconds(1)),
    ONE_MIN(Duration.ofMinutes(1)),
    FIVE_MIN(Duration.ofMinutes(5)),
    FIFTEEN_MIN(Duration.ofMinutes(15)),
    THIRTY_MIN(Duration.ofMinutes(30)),
    ONE_HOUR(Duration.ofHours(1)),
    TWO_HOURS(Duration.ofHours(2)),
    ONE_DAY(Duration.ofDays(1));

    companion object {
        fun of(duration: Duration): BarInterval {
            BarInterval.values()
                    .filter { it.duration.toMillis() == duration.toMillis() }
                    .forEach { return it }

            throw RuntimeException("Unknown duration $duration")
        }
    }


    override fun getDuration(): Duration {
        return _duration
    }


    override fun isDurationEstimated(): Boolean {
        return false
    }

    override fun isDateBased(): Boolean {
        return false
    }

    override fun isTimeBased(): Boolean {
        return true
    }

    override fun isSupportedBy(temporal: Temporal): Boolean {
        return temporal.isSupported(this)
    }

    override fun <R : Temporal> addTo(temporal: R, amount: Long): R {
        return temporal.plus(amount, this) as R
    }

    override fun between(temporal1Inclusive: Temporal, temporal2Exclusive: Temporal): Long {
        return temporal1Inclusive.until(temporal2Exclusive, this)
    }

    override fun toString(): String {
        return name
    }
}