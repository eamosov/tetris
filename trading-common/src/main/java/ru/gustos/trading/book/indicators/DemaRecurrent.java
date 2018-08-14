package ru.gustos.trading.book.indicators;

public class DemaRecurrent {
    EmaRecurrent ema1;
    EmaRecurrent ema2;
    EmaRecurrent emas;
    EmaRecurrent dema1;
    EmaRecurrent dema2;
    EmaRecurrent demas;
    double pvalue, value;

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

        return value();
    }

    public double value() {

        return value/ema2.value();
    }

    public double pvalue() {
        return pvalue/ema2.pvalue();
    }
}
