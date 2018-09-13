package ru.gustos.trading.global;

import kotlin.Pair;
import ru.efreet.trading.bars.MarketBar;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;
import ru.efreet.trading.exchange.Exchange;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.book.BarsSource;
import ru.gustos.trading.global.timeseries.TimeSeries;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public class InstrumentData implements BarsSource{
    public TimeSeries<XBar> bars;
    public ArrayList<MarketBar> marketBars;
    public ArrayList<MomentData> data;
    public ArrayList<MomentData> buydata;
    public ArrayList<MomentData> selldata;
    public BitSet buys = new BitSet();
    public BitSet sells = new BitSet();

    public Instrument instrument;
    public Exchange exchange;
    public MomentDataHelper helper;
    public MomentDataHelper buyhelper;
    public MomentDataHelper sellhelper;
    public Global global;

    XBaseBar totalBar;

    boolean withml, withbuysell;

    public InstrumentData(Exchange exch, Instrument instr, List<? extends XBar> bars, List<? extends MarketBar> marketBars, Global global, boolean withml, boolean withbuysell){
        exchange = exch;
        instrument = instr;
        this.withml = withml;
        this.withbuysell = withbuysell;
        this.global = global;
        this.bars = new TimeSeries<>(bars.size());
        this.marketBars = new ArrayList<>();
        totalBar = null;
        if (withml) {
            data = new ArrayList<>();
            helper = new MomentDataHelper();
            if (withbuysell) {
                buydata = new ArrayList<>();
                selldata = new ArrayList<>();
                buyhelper = new MomentDataHelper();
                sellhelper = new MomentDataHelper();
            }
        }

        for (int i = 0;i<bars.size();i++)
            addBar(bars.get(i), marketBars==null?null:marketBars.get(i));

    }

    public InstrumentData(InstrumentData data, int count){
        this(data.exchange,data.instrument, data.getBars(count), data.marketBars.subList(0,count),data.global, data.withml, data.withbuysell);
    }

    private List<XBar> getBars(int count) {
        return bars.direct().stream().limit(count).collect(Collectors.toList());
    }

    public void addBar(XBar bar, MarketBar marketBar){
        if (totalBar==null)
            totalBar = new XBaseBar(bar);
        else
            totalBar.addBar(bar);
        if (marketBars!=null)
            marketBars.add(marketBar);
        bars.add(bar, bar.getEndTime().toEpochSecond());
        if (withml) {
            data.add(new MomentData(100));
            if (withbuysell) {
                buydata.add(new MomentData(600));
                selldata.add(new MomentData(10));
            }
        }
    }

    public void resetMlData() {
        if (withml) {
            data = new ArrayList<>();
            helper = new MomentDataHelper();
            if (withbuysell) {
                buydata = new ArrayList<>();
                selldata = new ArrayList<>();
                buyhelper = new MomentDataHelper();
                sellhelper = new MomentDataHelper();
            }
            for (int i = 0;i<bars.size();i++){
                data.add(new MomentData(100));
                if (withbuysell) {
                    buydata.add(new MomentData(600));
                    selldata.add(new MomentData(10));
                }
            }
        }
    }

    public long getBeginTime() {
        return bars.getBeginTime();
    }

    public long getEndTime() {
        return bars.getEndTime();
    }

    @Override
    public int size() {
        return bars.size();
    }

    @Override
    public XBar bar(int index) {
        return bars.get(index);
    }

    public List<MomentData> data(){return data;}
    public List<MomentData> buydata(){return buydata;}
    public List<MomentData> selldata(){return selldata;}

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
    public double maxPrice;
    public double closePrice;
    GustosLogicStrategy(){
    }

    @Override
    public Pair<Double, Integer> calcProfit(InstrumentData data, int from) {
        double buy = data.bar(from).getClosePrice();
        maxPrice = buy;
        for (int i = from+1;i<data.size();i++){
            float closePrice = data.bars.get(i).getClosePrice();
            maxPrice = Math.max(maxPrice, closePrice);
            if (data.sells.get(i)) {
                this.closePrice = closePrice;
                return new Pair<>(this.closePrice / buy - 0.002, i);
            }

        }
        return new Pair<>(1.0, Integer.MAX_VALUE);
    }

    public int nextSell(InstrumentData data, int from){
        for (int i = from+1;i<data.size();i++)
            if (data.sells.get(i))
                return i;

        return Integer.MAX_VALUE;
    }
}

