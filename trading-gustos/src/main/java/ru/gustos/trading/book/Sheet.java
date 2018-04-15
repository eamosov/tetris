package ru.gustos.trading.book;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;
import ru.efreet.trading.utils.BarsPacker;
import ru.gustos.trading.book.indicators.*;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Exchange;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.impl.Binance;
import ru.efreet.trading.exchange.impl.cache.BarsCache;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class Sheet {
    Exchange exch;
    Instrument instr;
    BarInterval interval;

    public ArrayList<Moment> moments = new ArrayList<>();
    IndicatorsLib indicatorsLib;
    IndicatorsData indicatorsData;
//    IndicatorsDb indicatorsDb;

    public Sheet()throws Exception {
        this(new IndicatorsLib("indicators.json"));
    }

    public Sheet(IndicatorsLib lib) {
        this(new Binance(), Instrument.Companion.getBTC_USDT(), BarInterval.ONE_MIN,lib);
    }

    public Sheet(Exchange exch, Instrument instr, BarInterval interval, IndicatorsLib lib) {
        this.exch = exch;
        this.instr = instr;
        this.interval = interval;
        indicatorsLib = lib;
        indicatorsData = new IndicatorsData(this);
    }

    public Exchange exchange(){
        return exch;
    }

    public Instrument instrument(){
        return instr;
    }

    public BarInterval interval(){
        return interval;
    }

    public void fromExchange(){
        BarsCache cache = new BarsCache("cache.sqlite3");
        String exchName = exch.getName();
        cache.createTable(exchName, instr, interval);
        ZonedDateTime from = ZonedDateTime.of(2017,9,1,0,0,0,0, ZoneId.systemDefault());
        List<XBar> bars = exch.loadBars(instr, interval, from, ZonedDateTime.now());
        bars.removeIf((b)->BarInterval.Companion.ofSafe(b.getTimePeriod())!=interval);
        cache.saveBars(exchName, instr,bars);
        fromBars(bars);
    }

    public void fromCache() {
        fromCache(-1);
    }
    public void fromCache(int packing) {
//        indicatorsDb = new IndicatorsDb("d:\\tetrislibs\\inds");
//        indicatorsDb.updateTable(this);
        BarsCache cache = new BarsCache("cache.sqlite3");
        String exchName = exch.getName();
        ZonedDateTime from = ZonedDateTime.of(2017,11,1,0,0,0,0, ZoneId.systemDefault());
        List<XBaseBar> bars = cache.getBars(exchName, instr, interval, from, ZonedDateTime.now());
        if (packing>0)
            bars = BarsPacker.packBars(bars,packing);
        fromBars(bars);
    }

    public void fromBars(List<? extends XBar> bars){
        moments.clear();
        for (int i =0 ;i<bars.size();i++)
            moments.add(new Moment(bars.get(i)));
        calcIndicators();
    }


    public void calcIndicators(){
        for (IIndicator ii : indicatorsLib.listIndicators())
            indicatorsData.calc(ii);
    }

    public void calcIndicatorsNoPredict(){
        for (IIndicator ii : indicatorsLib.listIndicators())
            if (ii.getId()<200)
                indicatorsData.calc(ii);
//        try {
//            indicatorsDb.clear(this);
//            indicatorsData.save(indicatorsDb);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

//    public void loadIndicators() throws SQLException {
//        indicatorsData.load(indicatorsDb);
//    }

    private void fixMoments() {
        ZonedDateTime startTime = moments.get(0).bar.getBeginTime();
        int fixes = 0;
        for (int i = 1;i<moments.size();i++){
            ZonedDateTime t = moments.get(i).bar.getBeginTime();
            ZonedDateTime tt = startTime.plus(interval.getDuration().multipliedBy(i));
            Duration between = Duration.between(t, tt);
            long secs = between.getSeconds();
            int intervalSeconds = (int)interval.getDuration().getSeconds();
            if (secs<0 && secs< -intervalSeconds /2){
                int toInsert = (int)(-secs+ intervalSeconds /3)/ intervalSeconds;
                for (int j = 0;j<toInsert;j++)
                    InsertEmpty(i+j, tt.plus(interval.getDuration().multipliedBy(j)));
                System.out.println("inserted "+toInsert+" at "+i+" deltaSec "+secs);
                fixes++;
            } else if (secs!=0 && Math.abs(secs)< intervalSeconds /3) {
                moments.get(i).bar.setBeginTime(tt);
                moments.get(i).bar.setEndTime(tt.plus(interval.getDuration()));
                fixes++;
            } else if (secs>= intervalSeconds /3)
                System.out.println("negative step "+ between +" at "+i);
        }
        System.out.println("fixes: "+fixes);
    }

    private void InsertEmpty(int ind, ZonedDateTime time) {
        XBar bar = moments.get(ind).bar;
        XBar prev = moments.get(ind-1).bar;
        double price = prev.getClosePrice();

        Moment m = new Moment(new XBaseBar(interval.getDuration(),time.plus(interval.getDuration()), price, price, price, price,0));
        moments.add(ind,m);
    }

    @Override
    public String toString() {
        return String.format("sheet: size=%1$d",moments.size());
    }

    public ZonedDateTime getFrom() {
        return moments.get(0).bar.getBeginTime();
    }

    public int getBarIndex(ZonedDateTime from) {
        for (int i = 0;i<moments.size();i++)
            if (moments.get(i).bar.getBeginTime().isAfter(from))
                return i;
        return moments.size()-1;
    }

    public XBaseBar getSumBar(int fromInd, int bars) {
        XBaseBar bar = new XBaseBar(moments.get(fromInd).bar);
        for (int i = fromInd+1;i<Math.min(fromInd+bars,moments.size());i++)
            bar.addBar(moments.get(i).bar);
        return bar;
    }

    public IndicatorsLib getLib(){
        return indicatorsLib;
    }

    public IndicatorsData getData(){
        return indicatorsData;
    }

}

