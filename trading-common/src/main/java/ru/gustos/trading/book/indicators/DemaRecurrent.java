package ru.gustos.trading.book.indicators;

import java.util.ArrayList;

public class DemaRecurrent {
    EmaRecurrent ema1;
    EmaRecurrent ema2;
    EmaRecurrent emas;
    EmaRecurrent dema1;
    EmaRecurrent dema2;
    EmaRecurrent demas;
    double pvalue, value;
    ArrayList<Double> history = new ArrayList<>();

    public DemaRecurrent(int shortEma, int longEma, int signalEma) {
        ema1 = new EmaRecurrent(shortEma);
        ema2 = new EmaRecurrent(longEma);
        emas = new EmaRecurrent(signalEma);

        dema1 = new EmaRecurrent(shortEma);
        dema2 = new EmaRecurrent(longEma);
        demas = new EmaRecurrent(signalEma);

    }

    public double feed(double v) {
        if (!ema1.started()) {
            ema1.feed(v);
            ema2.feed(v);
            emas.feed(0);
            dema1.feed(v);
            dema2.feed(v);
            demas.feed(0);
            history.add(0.0);
            return 0;
        }
        ema1.feed(v);
        ema2.feed(v);
        dema1.feed(ema1.value());
        dema2.feed(ema2.value());

        double d1 = ema1.value()*2-dema1.value();
        double d2 = ema2.value()*2-dema2.value();
        double macd = d2 - d1;
        emas.feed(macd);
        demas.feed(emas.value());
        double ds = emas.value()*2 - demas.value();
        pvalue = value;
        value = (macd - ds);

        double result = value();
        history.add(result);
        if (history.size()>30)
            history.remove(0);
        return result;
    }

    public double value() {

        return value/ema2.value();
    }

    public double history(int back){
        if (history.size()==0) return 0;
        int ind = history.size()-back-1;
        if (ind<0) ind = 0;
        return history.get(ind);
    }

    public double pvalue() {
        return pvalue/ema2.pvalue();
    }
}
