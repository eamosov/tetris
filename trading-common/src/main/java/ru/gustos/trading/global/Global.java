package ru.gustos.trading.global;

import ru.efreet.trading.bars.MarketBar;
import ru.efreet.trading.bars.MarketBarFactory;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.global.timeseries.TimeSeries;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

public class Global {
    public Hashtable<String, InstrumentData> sheets = new Hashtable<>();
    TimeSeries<GlobalMoment> globalData = new TimeSeries<>();

    public PLHistoryAnalyzer planalyzer1 = new PLHistoryAnalyzer(false);
    public PLHistoryAnalyzer planalyzer2 = new PLHistoryAnalyzer(false);
    public PLHistoryAnalyzer planalyzer3 = new PLHistoryAnalyzer(false);


    public long minTime;
    public long maxTime;
    int timeStep;
    public List<MarketBar> marketBars;

    public Global() {
        minTime = Long.MAX_VALUE;
        maxTime = 0;
        timeStep = 30 * 60;
    }

    public InstrumentData getInstrument(String key) {
        return sheets.get(key);
    }

    public void addInstrumentData(String name, InstrumentData data) {
        sheets.put(name, data);
        if (minTime > data.getBeginTime())
            minTime = data.getBeginTime();
        if (maxTime < data.getEndTime())
            maxTime = data.getEndTime();
    }

    public void calcData() {
        long time = minTime;
        InstrumentData[] ss = sheets.values().toArray(new InstrumentData[0]);
        do {
            GlobalMoment m = new GlobalMoment();
            globalData.add(m, time);

//            calcChange(ss, m, time);

            time += timeStep;
        } while (time < maxTime);
    }

    static int intervals[] = new int[]{600, 3600, 4 * 3600, 24 * 3600, 7 * 24 * 3600};

    public void setMarket(List<MarketBar> marketBars) {
        this.marketBars = marketBars;
    }

    public int marketBarIndex(ZonedDateTime end) {
        for (int i = 0; i < marketBars.size(); i++)
            if (marketBars.get(i).getEndTime().isAfter(end)) return i-1;
        throw new NullPointerException("no market bar for this end time");
    }
}

class GlobalMoment {
    double[] mins;
    double[] maxes;
    double[] perc20;
    double[] perc50;
    double[] perc80;
    double[] positive;

    GlobalMoment() {
        mins = new double[Global.intervals.length];
        maxes = new double[Global.intervals.length];
        perc20 = new double[Global.intervals.length];
        perc50 = new double[Global.intervals.length];
        perc80 = new double[Global.intervals.length];
        positive = new double[Global.intervals.length];
    }

}

