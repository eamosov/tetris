package ru.efreet.trading.book.indicators;

import ru.efreet.trading.book.Sheet;

import java.awt.*;

public class TargetSellIndicator implements IIndicator{
    public static final int Id = 2;
    public static final Color COLOR = new Color(192,0,0);

    @Override
    public int getId() {
        return Id;
    }

    @Override
    public String getName() {
        return "Sell";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        for (int i = 0;i<values.length;i++)
            values[i] =  sheet.moments.get(i).decision==Decision.SELL?IIndicator.YES:Double.NaN;
    }

    @Override
    public Color getColorMax() {
        return COLOR;
    }

    public Color getColorMin() {
        return Color.darkGray;
    }

}

