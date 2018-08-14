package ru.gustos.trading.global;

import ru.gustos.trading.global.timeseries.TimeSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

public class Global {
    public Hashtable<String, InstrumentData> sheets = new Hashtable<>();
    TimeSeries<GlobalMoment> globalData = new TimeSeries<>();

    public PLHistoryAnalyzer planalyzer1 = new PLHistoryAnalyzer(false);
    public PLHistoryAnalyzer planalyzer2 = new PLHistoryAnalyzer(false);
    public PLHistoryAnalyzer planalyzer3 = new PLHistoryAnalyzer(false);



    public long minTime;
    public long maxTime;
    int timeStep;

    public Global() {
        minTime = Long.MAX_VALUE;
        maxTime = 0;
        timeStep = 30 * 60;
    }

    public InstrumentData getInstrument(String key){
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

    private void calcChange(InstrumentData[] ss, GlobalMoment m, long time) {

        double[][] ch = new double[intervals.length][];
        for (int j = 0; j < intervals.length; j++) {
            ArrayList<Double> ll = new ArrayList<>();
            for (int i = 0; i < ss.length; i++) {
                if (ss[i].getBeginTime()+intervals[j]<time)
                    ll.add(ss[i].getChange(time, intervals[j]));
            }
            ch[j] = ll.stream().mapToDouble(d->d).toArray();
        }

        for (int j = 0; j < intervals.length; j++) {
            Arrays.sort(ch[j]);
            int positive = 0;
            for (int i = 0;i<ch[j].length;i++)
               if (ch[j][i]>0) positive++;
            m.mins[j] = ch[j][0];
            int n = ch[j].length;
            m.maxes[j] = ch[j][n-1];
            m.perc50[j] = ch[j][n/2];
            m.perc20[j] = ch[j][n/5];
            m.perc80[j] = ch[j][n*4/5];
            m.positive[j] = ((double)positive)/ch[j].length;
        }
    }
}

class GlobalMoment {
    double[] mins;
    double[] maxes;
    double[] perc20;
    double[] perc50;
    double[] perc80;
    double[] positive;

    GlobalMoment(){
        mins = new double[Global.intervals.length];
        maxes = new double[Global.intervals.length];
        perc20 = new double[Global.intervals.length];
        perc50 = new double[Global.intervals.length];
        perc80 = new double[Global.intervals.length];
        positive = new double[Global.intervals.length];
    }

}

