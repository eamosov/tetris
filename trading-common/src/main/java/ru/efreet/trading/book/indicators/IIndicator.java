package ru.efreet.trading.book.indicators;

import ru.efreet.trading.book.Sheet;

import java.awt.*;

public interface IIndicator {
    double YES = 1.0;
    double NO = -1.0;

    int getId();
    String getName();
    IndicatorType getType();

    default boolean fromZero() {
        return false;
    }

    Color getColorMax();
    Color getColorMin();

    void calcValues(Sheet sheet, double[] values);
}

