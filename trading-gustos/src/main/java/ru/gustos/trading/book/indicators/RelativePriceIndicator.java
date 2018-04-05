package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class RelativePriceIndicator implements IIndicator{
    public static final int Id = 20;

    IndicatorPeriod period;

    public RelativePriceIndicator(IndicatorPeriod period){
        this.period = period;
    }

    @Override
    public int getId() {
        return Id + period.ordinal();
    }

    @Override
    public String getName() {
        return "RelativePrice_"+period.name();
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        int bars = IndicatorUtils.bars(period,sheet);
//        if (bars<20) {
            for (int i = 0; i < values.length; i++){
                XBar bar = sheet.moments.get(i).bar;
                double min = bar.getOpenPrice();
                double max = bar.getOpenPrice();
                for (int j = Math.max(0,i-bars);j<i;j++) {
                    XBar b = sheet.moments.get(j).bar;
                    if (min>b.getMinPrice()) min = b.getMinPrice();
                    if (max<b.getMaxPrice()) max = b.getMaxPrice();
                }
                if (max-min<0.001)
                    values[i] = 0.5;
                else
                    values[i] = (bar.getOpenPrice()-min)/(max-min);
            }
//        }
    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }

    public Color getColorMin() {        return CandlesPane.RED;    }

    @Override
    public boolean fromZero() {
        return true;
    }
}

