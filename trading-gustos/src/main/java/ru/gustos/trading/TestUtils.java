package ru.gustos.trading;

import ru.efreet.trading.bars.XBaseBar;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Exchange;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.impl.Binance;
import ru.efreet.trading.exchange.impl.cache.BarsCache;
import ru.efreet.trading.utils.BarsPacker;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.IndicatorsLib;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class TestUtils {

    public static Sheet makeSheet() throws Exception {
        Exchange exch = new Binance();
        Instrument instr = Instrument.Companion.getBTC_USDT();
        BarInterval interval = BarInterval.ONE_MIN;

        BarsCache cache = new BarsCache("cache.sqlite3");
//        ZonedDateTime from = ZonedDateTime.of(2018,4,15,0,0,0,0, ZoneId.systemDefault());
        ZonedDateTime from = ZonedDateTime.of(2017,11,1,0,0,0,0, ZoneId.systemDefault());
        List<XBaseBar> bars = cache.getBars(exch.getName(), instr, interval, from, ZonedDateTime.now());

// индикаторы
        IndicatorsLib lib = new IndicatorsLib("indicators.json");

        Sheet sheet = new Sheet(exch,instr,interval, lib);
//        bars = BarsPacker.packBarsVolume(bars,100);
//        bars = BarsPacker.packBarsSign(bars);
        sheet.fromBars(bars);
        return sheet;
    }

    public static Sheet makeSheetBcc() throws Exception {
        Exchange exch = new Binance();
        Instrument instr = Instrument.Companion.getBCC_USDT();
        BarInterval interval = BarInterval.ONE_MIN;

        BarsCache cache = new BarsCache("cache.sqlite3");
        ZonedDateTime from = ZonedDateTime.of(2017,11,1,0,0,0,0, ZoneId.systemDefault());
        List<XBaseBar> bars = cache.getBars(exch.getName(), instr, interval, from, ZonedDateTime.now());

// индикаторы
        IndicatorsLib lib = new IndicatorsLib("indicators.json");

        Sheet sheet = new Sheet(exch,instr,interval, lib);
//        bars = BarsPacker.packBarsVolume(bars,100);
//        bars = BarsPacker.packBarsSign(bars);
        sheet.fromBars(bars);
        return sheet;
    }

    public static Sheet makeSheet(String libfile) throws Exception {
        Exchange exch = new Binance();
        Instrument instr = Instrument.Companion.getBTC_USDT();
        BarInterval interval = BarInterval.ONE_MIN;

        BarsCache cache = new BarsCache("cache.sqlite3");
//        ZonedDateTime from = ZonedDateTime.of(2017,11,1,0,0,0,0, ZoneId.systemDefault());
        ZonedDateTime from = ZonedDateTime.of(2018,2,1,0,0,0,0, ZoneId.systemDefault());
        List<XBaseBar> bars = cache.getBars(exch.getName(), instr, interval, from, ZonedDateTime.now());

// индикаторы
        IndicatorsLib lib = libfile==null?new IndicatorsLib():new IndicatorsLib(libfile);

        Sheet sheet = new Sheet(exch,instr,interval, lib);
//        if (pack>0)
//            bars = BarsPacker.packBarsVolume(bars,pack);
//        bars = BarsPacker.packBarsSign(bars);
//        bars = BarsPacker.packBars(bars,15);
        sheet.fromBars(bars);
        return sheet;
    }
}
