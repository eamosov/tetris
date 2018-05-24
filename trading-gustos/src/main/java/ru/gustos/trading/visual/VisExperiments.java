package ru.gustos.trading.visual;

import ru.gustos.trading.TestUtils;

public class VisExperiments {
    public static void main(String[] args) throws Exception {
        Visualizator vis = new Visualizator(TestUtils.makeSheet("indicators_simple.json"));
//        vis.updateSelectedIndicator(12);
//        TradeHistory h = TradeHistory.Companion.loadFromJson("simulate_history.json");
//        TestUtils.makeVisualizator("indicators_simple.json",Instrument.getBTC_USDT(),h);

    }
}
