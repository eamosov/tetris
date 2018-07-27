package ru.gustos.trading.global;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;
import ru.efreet.trading.exchange.Exchange;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.book.BarsSource;
import ru.gustos.trading.global.timeseries.TimeSeries;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InstrumentData implements BarsSource{
    public TimeSeries<InstrumentMoment> bars;
    public Instrument instrument;
    public Exchange exchange;
    MomentDataHelper helper;
    Global global;

    XBaseBar totalBar;

    public InstrumentData(Exchange exch, Instrument instr, List<? extends XBar> bars, Global global){
        exchange = exch;
        instrument = instr;
        this.global = global;
        this.bars = new TimeSeries<>(bars.size());
        totalBar = null;
        for (int i = 0;i<bars.size();i++)
            addBar(bars.get(i));

        helper = new MomentDataHelper();
    }

    public InstrumentData(InstrumentData data, int count){
        this(data.exchange,data.instrument, data.getBars(count),data.global);
    }

    private List<XBar> getBars(int count) {
        return bars.direct().stream().limit(count).map(d->d.bar).collect(Collectors.toList());
    }

    public void addBar(XBar bar){
        if (totalBar==null)
            totalBar = new XBaseBar(bar);
        else
            totalBar.addBar(bar);
        bars.add(new InstrumentMoment(bar), bar.getEndTime().toEpochSecond());
    }


    public long getBeginTime() {
        return bars.getBeginTime();
    }

    public long getEndTime() {
        return bars.getEndTime();
    }

    public double getChange(long time, int interval) {
        InstrumentMoment m1 = bars.getAt(time);
        InstrumentMoment m2 = bars.getAt(time-interval);
        if (m1==null || m2==null) return 1;
        return m1.bar.getClosePrice()/m2.bar.getClosePrice();
    }

    @Override
    public int size() {
        return bars.size();
    }

    @Override
    public XBar bar(int index) {
        return bars.get(index).bar;
    }

    public List<InstrumentMoment> moments(){return bars.direct();}

    @Override
    public XBar totalBar() {
        return totalBar;
    }

    @Override
    public int getBarIndex(ZonedDateTime time) {
        return getBarIndex(time.toEpochSecond());
    }

    public int getBarIndex(long time) {
        return bars.findIndex(time);
    }
    public XBar getBarAt(long time) {
        int index = bars.findIndex(time);
        if (index<0) return null;
        return bar(index);
    }
}


interface MomentDataProvider {
    MomentData getMomentData();
}
class InstrumentMoment implements  MomentDataProvider{
    XBar bar;
    MomentData mldata;

    public InstrumentMoment(XBar bar) {
        this.bar = bar;
        mldata = new MomentData(90);
    }


    @Override
    public MomentData getMomentData() {
        return mldata;
    }
}

interface PlayStrategy{
    Pair<Double,Integer> calcProfit(InstrumentData data, int from);
}

class SimpleStrategy implements  PlayStrategy{
    double target,sl;

    SimpleStrategy(double target, double sl){
        this.target = target;
        this.sl = sl;
    }

    @Override
    public Pair<Double, Integer> calcProfit(InstrumentData data, int from) {
        double buy = data.bar(from).getClosePrice();
        double target = buy*this.target;
        double sl = buy*this.sl;
        for (int i = from+1;i<data.size();i++){
            XBar bar = data.bar(i);
            if (bar.getClosePrice()<=sl || bar.getClosePrice()>=target)
                return new Pair<>(bar.getClosePrice()/buy, i);
        }

        return new Pair<>(1.0, Integer.MAX_VALUE);
    }
}

class SimpleStrategyWithBackup implements PlayStrategy {
    double target,sl;
    int backups;

    SimpleStrategyWithBackup(double target, double sl, int backups){
        this.target = target;
        this.sl = sl;
        this.backups = backups;
    }

    @Override
    public Pair<Double, Integer> calcProfit(InstrumentData data, int from) {
        int used = 0;
        double buy = data.bar(from).getClosePrice();
        double target = buy*this.target;
        double sl = buy*this.sl;
        for (int i = from+1;i<data.size();i++){
            XBar bar = data.bar(i);
            if ((bar.getClosePrice()<=sl && used==backups) || bar.getClosePrice()>=target)
                return new Pair<>((bar.getClosePrice()/buy-1)*(1+used)/(1+backups)+1, i);
            if (bar.getClosePrice()<=sl) {
                buy = (buy*(used+1) + bar.getClosePrice())/(used+2);
                target = buy*this.target;
                sl*=this.sl;
                used++;
            }
        }

        return new Pair<>(1.0, Integer.MAX_VALUE);
    }
}

class GustosLogicStrategy implements PlayStrategy{
    GustosLogicStrategy(){
    }

    @Override
    public Pair<Double, Integer> calcProfit(InstrumentData data, int from) {
        double buy = data.bar(from).getClosePrice();
        for (int i = from+1;i<data.size();i++){
            if (data.helper.get(data.bars.get(i).mldata,"gustosSell")==1.0 ){
                return new Pair<>(data.bars.get(i).bar.getClosePrice()/buy,i);
            }
        }
        return new Pair<>(1.0, Integer.MAX_VALUE);
    }

    public int nextSell(InstrumentData data, int from){
        for (int i = from+1;i<data.size();i++)
            if (data.helper.get(data.bars.get(i).mldata,"gustosSell")==1.0 )
                return i;

        return Integer.MAX_VALUE;
    }
}
