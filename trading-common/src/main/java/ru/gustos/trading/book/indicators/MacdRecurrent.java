package ru.gustos.trading.book.indicators;

public class MacdRecurrent {
    EmaRecurrent shorte;
    EmaRecurrent longe;
    EmaRecurrent signale;

    public MacdRecurrent(int shortEma, int longEma, int signalEma) {
        shorte = new EmaRecurrent(shortEma);
        longe = new EmaRecurrent(longEma);
        signale = new EmaRecurrent(signalEma);
    }

    public double feed(double v) {
        signale.feed(longe.feed(v) - shorte.feed(v));
        return value();
    }

    public double value() {
        return ((longe.value() - shorte.value()) - signale.value()) / longe.value() * 10;
    }
    public double pvalue() {
        return ((longe.pvalue() - shorte.pvalue()) - signale.pvalue()) / longe.pvalue() * 10;
    }
}

