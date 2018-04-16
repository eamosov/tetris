package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

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

    default boolean showOnPane() { return true; }

    Color getColorMax();
    Color getColorMin();

    void calcValues(Sheet sheet, double[] values);
}
