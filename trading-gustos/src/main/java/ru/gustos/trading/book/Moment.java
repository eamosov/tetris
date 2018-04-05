package ru.gustos.trading.book;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.indicators.Decision;

public class Moment {
    public XBar bar;
    public Decision decision;
    public Decision decisionRisky;


    public Moment(XBar bar) {
        this.bar = bar;
        decision = Decision.NONE;
        decisionRisky = Decision.NONE;
    }
}

