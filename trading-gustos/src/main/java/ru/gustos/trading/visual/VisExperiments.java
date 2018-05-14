package ru.gustos.trading.visual;

import ru.efreet.trading.bars.XBaseBar;
import ru.efreet.trading.bot.TradeHistory;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Exchange;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.impl.Binance;
import ru.efreet.trading.exchange.impl.cache.BarsCache;
import ru.efreet.trading.utils.BarsPacker;
import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.IndicatorType;
import ru.gustos.trading.book.indicators.IndicatorsLib;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

public class VisExperiments {
    public static void main(String[] args) throws Exception {
//        Visualizator vis = new Visualizator(TestUtils.makeSheet());
//        vis.updateSelectedIndicator(12);
        TradeHistory h = TradeHistory.Companion.loadFromJson("simulate_history.json");
        TestUtils.makeVisualizator("indicators_simple.json",Instrument.getBTC_USDT(),h);

    }
}
