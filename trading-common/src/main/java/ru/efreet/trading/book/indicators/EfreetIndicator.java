package ru.efreet.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.book.Moment;
import ru.efreet.trading.book.Sheet;

import java.awt.*;

public class EfreetIndicator implements IIndicator {
    public static final int Id = 5;

    @Override
    public int getId() {
        return Id;
    }

    @Override
    public String getName() {
        return "efreet";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public Color getColorMax() {
        return Color.green;
    }

    @Override
    public Color getColorMin() {
        return Color.red;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        for (int i = 0;i<values.length;i++) {
            XBar bar = sheet.moments.get(i).bar;
            Decision decision = Decision.BUY;
            values[i] =  decision==Decision.BUY?IIndicator.YES:(decision==Decision.SELL?IIndicator.NO:Double.NaN);
        }

    }
}
