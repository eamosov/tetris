package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class MaxPriceChangeIndicator extends BaseIndicator {
    IndicatorPeriod period;
    boolean positive;

    public MaxPriceChangeIndicator(IndicatorInitData data){
        super(data);
        positive = data.positive;
        period =  IndicatorPeriod.values()[data.period];
    }

    @Override
    public String getName() {
        return "MaxPriceChange_"+period.name()+"_"+(positive?"pos":"neg");
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        int bars = IndicatorUtils.bars(period,sheet);
        if (positive) {
            for (int i = bars; i < values.length; i++) {
                double cur = sheet.moments.get(i).bar.getClosePrice();
                double min = cur;
                for (int j = 0;j<=bars;j++){
                    double v = sheet.moments.get(i-bars+j).bar.getMinPrice();
                    if (v<min) min = v;
                }
                values[i] = (cur/min-1)*10;

            }
        } else {
            for (int i = bars; i < values.length; i++) {
                double cur = sheet.moments.get(i).bar.getClosePrice();
                double max = cur;
                for (int j = 0;j<=bars;j++){
                    double v = sheet.moments.get(i-bars+j).bar.getMaxPrice();
                    if (v>max) max = v;
                }
                values[i] = (max/cur-1)*10;

            }
        }
    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }

    @Override
    public boolean fromZero() {
        return true;
    }

    public Color getColorMin() {        return Color.black;    }
}


