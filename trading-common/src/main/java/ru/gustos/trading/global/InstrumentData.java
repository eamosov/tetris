package ru.gustos.trading.global;

import kotlin.Pair;
import ru.efreet.trading.bars.MarketBar;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;
import ru.efreet.trading.exchange.Exchange;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.book.BarsSource;
import ru.gustos.trading.global.timeseries.TimeSeries;
import weka.classifiers.Classifier;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    public ArrayList<MomentData> resultdata;
    public BitSet buys = new BitSet();
    public BitSet sells = new BitSet();

    public Instrument instrument;
    public Exchange exchange;
    public MomentDataHelper helper;
    public MomentDataHelper buyhelper;
    public MomentDataHelper sellhelper;
    public MomentDataHelper resulthelper;

    public int[] minPow;
    public int[] maxPow;
    public double avgStep;


    XBaseBar totalBar;

    boolean withml, withbuysell;

    public InstrumentData(Exchange exch, Instrument instr, List<? extends XBar> bars, List<? extends MarketBar> marketBars, boolean withml, boolean withbuysell){
        exchange = exch;
        instrument = instr;
        this.withml = withml;
        this.withbuysell = withbuysell;
        this.bars = new TimeSeries<>(bars.size());
        this.marketBars = new ArrayList<>();
        totalBar = null;
        if (withml) {
            data = new ArrayList<>();
            helper = new MomentDataHelper();
            resultdata = new ArrayList<>();
            resulthelper = new MomentDataHelper();
            if (withbuysell) {
                buydata = new ArrayList<>();
                selldata = new ArrayList<>();
                buyhelper = new MomentDataHelper();
                sellhelper = new MomentDataHelper();
            }
        }

        avgStep = 0;
        for (int i = 0;i<bars.size();i++) {
            avgStep+=bars.get(i).deltaMaxMin();
            addBar(bars.get(i), marketBars == null ? null : marketBars.get(i));
        }
        avgStep/=bars.size();


    }

    InstrumentData(){
    }

    public InstrumentData(InstrumentData data, int count){
        this(data, count, data.withml, data.withbuysell);
    }

    public InstrumentData(InstrumentData data, int count, boolean withml, boolean withbuysell){
        this(data.exchange,data.instrument, data.getBars(count), data.marketBars.subList(0,count),withml, withbuysell);
    }

    public InstrumentData(String name) {
        instrument = Instrument.Companion.parse(name);
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
            data.add(new MomentData(130));
            resultdata.add(new MomentData(30));
            if (withbuysell) {
                buydata.add(new MomentData(600));
                selldata.add(new MomentData(10));
            }
        }
    }

    public XBaseBar getSumBar(int fromInd, int bars) {
        XBaseBar bar = new XBaseBar(bar(fromInd));
        for (int i = fromInd+1;i<Math.min(fromInd+bars,size());i++)
            bar.addBar(bar(i));
        return bar;
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

    public void saveData(ObjectOutputStream out) throws IOException {
        out.writeObject(buys);
        out.writeObject(sells);
        helper.save(out,data);
    }

    public void loadData(ObjectInputStream in) throws IOException, ClassNotFoundException {
        buys = (BitSet) in.readObject();
        sells = (BitSet) in.readObject();
        data = helper.load(in);

    }

    public void initMinMax(){
        if (minPow!=null) return;
        minPow = new int[size()];
        maxPow = new int[size()];

        for (int i = 60*24;i<size()-60*24;i++){
            double min = bar(i).getMinPrice();
            double max = bar(i).getMaxPrice();
            int minfound = 0;
            int maxfound = 0;

            for (int j = 1;j<60*24;j++){
                if (minfound==0) {
                    double before = bar(i-j).getMinPrice();
                    double after = bar(i+j).getMinPrice();
                    if (before<min || after<min){
                        minfound = j;
                    }
                }
                if (maxfound==0){
                    double before = bar(i-j).getMaxPrice();
                    double after = bar(i+j).getMaxPrice();
                    if (before>max || after>max){
                        maxfound = j;
                    }

                }
                if (minfound>0 && maxfound>0) break;
            }
            if (minfound==0) minfound = 60*24;
            if (maxfound==0) maxfound = 60*24;
            minPow[i] = 32-Integer.numberOfLeadingZeros(minfound-1);
            maxPow[i] = 32-Integer.numberOfLeadingZeros(maxfound-1);
        }
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

