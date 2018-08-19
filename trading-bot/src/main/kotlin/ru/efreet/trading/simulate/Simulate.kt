package ru.efreet.trading.simulate

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.efreet.trading.Graph
import ru.efreet.trading.utils.CmdArgs
import ru.efreet.trading.utils.storeAsJson

class Simulate {

    companion object {

        val log:Logger = LoggerFactory.getLogger(Simulate::class.java)

        @JvmStatic
        fun main(args: Array<String>) {

            val cmd = CmdArgs.parse(args)

            val sim = Simulator(cmd)

            val state = State.load("simulate.json")

            log.info("simulate.json: {}", state)

            val tradeHistory = sim.run(state)

            tradeHistory.storeAsJson(state.historyPath)

            if (state.graph) {
                Graph().drawHistory(tradeHistory)
            }
        }
    }

}