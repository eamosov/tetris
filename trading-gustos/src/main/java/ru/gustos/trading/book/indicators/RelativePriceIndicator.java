package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class RelativePriceIndicator extends BaseIndicator {
    IndicatorPeriod period;

    public RelativePriceIndicator(IndicatorInitData data){
        super(data);
        period =  IndicatorPeriod.values()[data.period];
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
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        int bars = IndicatorUtils.bars(period,sheet);
//        if (bars<20) {
            for (int i = from; i < to; i++){
                XBar bar = sheet.bar(i);
                double min = bar.getClosePrice();
                double max = bar.getClosePrice();
                for (int j = Math.max(0,i-bars);j<i;j++) {
                    XBar b = sheet.moments.get(j).bar;
                    if (min>b.getMinPrice()) min = b.getMinPrice();
                    if (max<b.getMaxPrice()) max = b.getMaxPrice();
                }
                if (max-min<0.001)
                    values[i] = 0.5;
                else
                    values[i] = (bar.getClosePrice()-min)/(max-min);
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

