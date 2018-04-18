package ru.gustos.trading.visual;

import ru.efreet.trading.bars.XBaseBar;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Exchange;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.impl.Binance;
import ru.efreet.trading.exchange.impl.cache.BarsCache;
import ru.efreet.trading.utils.BarsPacker;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.IndicatorType;
import ru.gustos.trading.book.indicators.IndicatorsLib;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

public class VisualizatorDemo{


    public static void main(String[] args) {
        Exchange exch = new Binance();
        Instrument instr = Instrument.Companion.getBTC_USDT();
        BarInterval interval = BarInterval.ONE_MIN;

        BarsCache cache = new BarsCache("cache.sqlite3");
        ZonedDateTime from = ZonedDateTime.of(2017,11,1,0,0,0,0, ZoneId.systemDefault());
        List<XBaseBar> bars = cache.getBars(exch.getName(), instr, interval, from, ZonedDateTime.now());

// индикаторы
        IndicatorsLib lib = new IndicatorsLib();

        Random r = new Random();
// быки медведи
        double[] rr = new double[bars.size()];
        for (int i = 0;i<rr.length;i++)
            rr[i] = r.nextInt(1000)==0?(bars.get(i).isBullish()?1:(bars.get(i).isBearish()?-1:0)):0;
        lib.add("bull bear", IndicatorType.YESNO,rr);

// объем
        rr = new double[bars.size()];
        for (int i = 0;i<rr.length;i++)
            rr[i] = bars.get(i).getVolume();
        lib.add("volume", IndicatorType.NUMBER,rr);


        Sheet sheet = new Sheet(exch,instr,interval,lib);
//        bars = BarsPacker.packBarsVolume(bars,100);
        sheet.fromBars(bars);

        new Visualizator(sheet);
    }
}
