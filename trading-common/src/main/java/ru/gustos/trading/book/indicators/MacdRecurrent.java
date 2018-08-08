package ru.gustos.trading.book.indicators;

public class MacdRecurrent {
    EmaRecurrent shorte;
    EmaRecurrent longe;
    EmaRecurrent signale;
    double v,pv;

    public MacdRecurrent(int shortEma, int longEma, int signalEma) {
        shorte = new EmaRecurrent(shortEma);
        longe = new EmaRecurrent(longEma);
        signale = new EmaRecurrent(signalEma);
    }

    public double feed(double price) {
        signale.feed(longe.feed(price) - shorte.feed(price));
        pv = v;
        v = (longe.value() - shorte.value()) - signale.value();
        return value();
    }

    public double value() {
        return v / longe.value();
    }
    public double pvalue() {
        return pv / longe.pvalue();
    }
}

