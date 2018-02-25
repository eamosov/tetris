package ru.efreet.trading.bot

import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.TradeRecord
import ru.efreet.trading.utils.loadFromJson
import ru.efreet.trading.utils.storeAsJson
import java.io.FileNotFoundException

/**
 * Created by fluder on 20/02/2018.
 */
data class BotSettings(val trades: MutableMap<String, MutableList<TradeRecord>> = mutableMapOf()
//                       , val params: MutableMap<String, SimpleBotLogicParams> = mutableMapOf()
) {


    fun getLastTrade(instrument: Instrument): TradeRecord? {
        return trades[instrument.toString()]?.lastOrNull()
    }

    fun addTrade(instrument: Instrument, trade: TradeRecord) {
        trades.computeIfAbsent(instrument.toString(), { mutableListOf() }).add(trade)
    }

//    fun getParams(instrument: Instrument): SimpleBotLogicParams? {
//        return params[instrument.toString()]
//    }
//
//    fun setParams(instrument: Instrument, value: SimpleBotLogicParams?) {
//        if (value == null)
//            params.remove(instrument.toString())
//        else
//            params[instrument.toString()] = value
//
//    }

    companion object {
        fun load(path: String): BotSettings {
            return try {
                return loadFromJson(path)
            } catch (e: FileNotFoundException) {
                val settings = BotSettings()
                settings.storeAsJson(path)
                settings
            }
        }

        fun save(path: String, settings: BotSettings) {
            settings.storeAsJson(path)
        }
    }
}