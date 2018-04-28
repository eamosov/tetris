package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public interface IIndicator {
    double YES = 1.0;
    double NO = -1.0;

    int getId();
    String getName();
    IndicatorType getType();

    default boolean fromZero() {
        return false;
    }

    default boolean showOnPane() { return !priceLine(); }
    default boolean priceLine() { return false; }

    default Map<String,String> getMark(int ind){ return new HashMap<>();}

    Color getColorMax();
    Color getColorMin();

    void calcValues(Sheet sheet, double[] values);
}

