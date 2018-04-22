package ru.efreet.trading.utils

import com.google.gson.TypeAdapter
import java.time.ZonedDateTime
import java.io.IOException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter



/**
 * Created by fluder on 20/04/2018.
 */
class ZonedDateTimeType : TypeAdapter<ZonedDateTime>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: ZonedDateTime) {
        out.value(value.toString())
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): ZonedDateTime {
        return ZonedDateTime.parse(`in`.nextString())
    }
}