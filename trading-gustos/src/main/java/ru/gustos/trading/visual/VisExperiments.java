package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.efreet.trading.bot.TradeHistory;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.indicators.VecUtils;
import ru.gustos.trading.book.ml.Exporter;
import ru.gustos.trading.global.ExperimentData;
import ru.gustos.trading.tests.TestGlobal;

import java.util.Arrays;

public class VisExperiments {
    public static Instrument[] instruments = new Instrument[]{
            new Instrument("BTC","USDT"),
            new Instrument("TRX","USDT"),
    };

    public static void main(String[] args) throws Exception {
        ExperimentData experiment = ExperimentData.load("experiment");
        Visualizator vis = new Visualizator(experiment);//TestUtils.makeSheet("indicators_simple.json",new Instrument("BTC", "USDT"))
//        Pair<double[], double[]> vv = vis.getSheet().volumes().get();
//        double[] sum = VecUtils.add(vv.getFirst(), vv.getSecond(), 1);
//        Exporter.string2file("d:\\delme\\sum", Arrays.toString(sum));

//        vis.updateSelectedIndicator(12);
//        TradeHistory h = TradeHistory.Companion.loadFromJson("simulate_history.json");
//        TestUtils.makeVisualizator("indicators_simple.json", Instrument.getBTC_USDT(),h);

    }
}

