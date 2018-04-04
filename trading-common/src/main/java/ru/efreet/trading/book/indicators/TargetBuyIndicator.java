package ru.efreet.trading.book.indicators;

import ru.efreet.trading.book.Sheet;
import ru.efreet.trading.visual.CandlesPane;

import java.awt.*;

public class TargetBuyIndicator implements IIndicator{
    public static final int Id = 1;

    @Override
    public int getId() {
        return Id;
    }

    @Override
    public String getName() {
        return "Buy";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        for (int i = 0;i<values.length;i++)
            values[i] =  sheet.moments.get(i).decision==Decision.BUY?IIndicator.YES:Double.NaN;
    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }

    @Override
    public Color getColorMin() {
        return Color.darkGray;
    }
}

