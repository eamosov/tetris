package ru.gustos.trading;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;
import ru.efreet.trading.bot.TradeHistory;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Exchange;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.impl.Binance;
import ru.efreet.trading.exchange.impl.cache.BarsCache;
import ru.efreet.trading.utils.BarsPacker;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.IndicatorsLib;
import ru.gustos.trading.book.indicators.SuccessIndicator;
import ru.gustos.trading.book.indicators.TradeHistoryTradesIndicator;
import ru.gustos.trading.visual.Visualizator;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class TestUtils {

    public static Sheet makeSheet() throws Exception {
        ZonedDateTime from = ZonedDateTime.of(2017,11,1,0,0,0,0, ZoneId.systemDefault());
        return makeSheet(from,ZonedDateTime.now());
    }
    public static Sheet makeSheet(ZonedDateTime from, ZonedDateTime to) throws Exception {
        Exchange exch = new Binance();
        Instrument instr = Instrument.Companion.getBTC_USDT();
        BarInterval interval = BarInterval.ONE_MIN;

        BarsCache cache = new BarsCache("cache.sqlite3");
        List<? extends XBar> bars = cache.getBars(exch.getName(), instr, interval, from, to);

// индикаторы
        IndicatorsLib lib = new IndicatorsLib("indicators.json");

        Sheet sheet = new Sheet(exch,instr,interval, lib);
//        bars = BarsPacker.invertBars(bars);
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
        List<? extends XBar> bars = cache.getBars(exch.getName(), instr, interval, from, ZonedDateTime.now());

// индикаторы
        IndicatorsLib lib = new IndicatorsLib("indicators.json");

        Sheet sheet = new Sheet(exch,instr,interval, lib);
        sheet.fromBars(bars);
        return sheet;
    }

    public static Sheet makeSheet(String libfile) throws Exception {
        return makeSheet(libfile, Instrument.Companion.getBTC_USDT());
//        return makeSheet(libfile, Instrument.Companion.getTHETA_BTC());
    }

    public static Sheet makeSheet(String libfile,Instrument instr) throws Exception {
        ZonedDateTime from = ZonedDateTime.of(2017,11,1,0,0,0,0, ZoneId.systemDefault());
        return makeSheet(libfile, instr, from, ZonedDateTime.now());
    }

    public static List<Instrument> getInstruments(){
        Exchange exch = new Binance();
        BarInterval interval = BarInterval.ONE_MIN;

        BarsCache cache = new BarsCache("cache.sqlite3");
        return cache.getInstruments(exch.getName(),interval);
    }

    public static double barsPack = 0;
    public static Sheet makeSheet(String libfile, Instrument instr, ZonedDateTime from, ZonedDateTime to) throws Exception {
        Exchange exch = new Binance();
        BarInterval interval = BarInterval.ONE_MIN;

        BarsCache cache = new BarsCache("cache.sqlite3");
        List<? extends XBar> bars = cache.getBars(exch.getName(), instr, interval, from, to);

        IndicatorsLib lib = libfile==null?new IndicatorsLib():new IndicatorsLib(libfile);

        Sheet sheet = new Sheet(exch,instr,interval, lib);
        bars = BarsPacker.packBarsVolumeEma(bars,1000,8.1);
//        bars = BarsPacker.packBarsVolumeAvg(bars,30);
        sheet.fromBars(bars);
        return sheet;
    }

    public static Sheet makeSheet(Instrument instr, TradeHistory history) throws Exception {
        return makeSheet(null, instr, history.getStart(), history.getEnd());
    }

    public static Visualizator makeVisualizator(String lib, Instrument instr, TradeHistory history) throws Exception {
        Sheet sheet = makeSheet(lib, instr, history.getStart(), history.getEnd());
        sheet.getLib().add(new TradeHistoryTradesIndicator(100,history));
        sheet.getLib().add(new SuccessIndicator(101,100));
        sheet.calcIndicators();
//        Visualizator vis = new Visualizator(sheet);
//        vis.updateSelectedIndicator(101);
//        vis.setPlayHistory(history);
        return null;
    }

    public static void main(String[] args) {
        Exchange exch = new Binance();
        BarInterval interval = BarInterval.ONE_MIN;
        ZonedDateTime from = ZonedDateTime.of(2017,11,1,0,0,0,0, ZoneId.systemDefault());
        Instrument instr = new Instrument("BCC", "USDT");
        BarsCache cache1 = new BarsCache("cache.sqlite3");
        BarsCache cache2 = new BarsCache("cache2.sqlite3");
        List<? extends XBar> bars1 = cache1.getBars(exch.getName(), instr, interval, from, ZonedDateTime.now());
        List<? extends XBar> bars2 = cache2.getBars(exch.getName(), instr, interval, from, ZonedDateTime.now());
        for (int i = 0;i<bars1.size();i++){
            XBar b1 = bars1.get(i);
            XBar b2 = bars2.get(i);
            if (!b1.equals(b2)){
                System.out.println("not equal "+ b1+" "+b2);
            }
        }

    }
}


