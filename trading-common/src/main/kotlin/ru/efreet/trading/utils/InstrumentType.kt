package ru.efreet.trading.utils

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import ru.efreet.trading.exchange.Instrument
import java.io.IOException


/**
 * Created by fluder on 20/04/2018.
 */
class InstrumentType : TypeAdapter<Instrument>() {

    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Instrument) {
        out.value(value.toString())
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Instrument {
        return Instrument.parse(`in`.nextString())
    }
}

