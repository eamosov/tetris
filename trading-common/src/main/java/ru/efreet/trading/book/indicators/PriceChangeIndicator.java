package ru.efreet.trading.book.indicators;

import ru.efreet.trading.book.Sheet;
import ru.efreet.trading.visual.CandlesPane;

import java.awt.*;


public class PriceChangeIndicator implements IIndicator{
    public static final int Id = 10;

    IndicatorPeriod period;

    public PriceChangeIndicator(IndicatorPeriod period){
        this.period = period;
    }

    @Override
    public int getId() {
        return Id + period.ordinal();
    }

    @Override
    public String getName() {
        return "PriceChange_"+period.name();
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        int bars = IndicatorUtils.bars(period,sheet);
        for (int i = 0;i<values.length;i++)
            values[i] =  sheet.moments.get(i).bar.getOpenPrice()-sheet.moments.get(Math.max(0,i-bars)).bar.getOpenPrice();
    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }

    public Color getColorMin() {        return CandlesPane.RED;    }
}

