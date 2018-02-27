package ru.efreet.trading.bot

/**
 * Created by fluder on 27/02/2018.
 */

data class BotConfig(var logic: String,
                     var settings: String,
                     var instrument: String,
                     var interval: String,
                     var limit:Double)

data class BotConfiguration(var bots: List<BotConfig>)