package ru.gustos.trading.book;

import ru.efreet.trading.bars.XBar;

import java.time.ZonedDateTime;

public interface BarsSource{
    int size();
    XBar bar(int index);
    XBar totalBar();

    int getBarIndex(ZonedDateTime time);
}
