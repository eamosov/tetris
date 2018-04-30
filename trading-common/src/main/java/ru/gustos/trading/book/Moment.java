package ru.gustos.trading.book;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.Decision;

public class Moment {
    public XBar bar;
    public Decision decision;
    public Decision decisionRisky;
    public double weight = 1;


    public Moment(XBar bar) {
        this.bar = bar;
        decision = Decision.NONE;
        decisionRisky = Decision.NONE;
    }
}

