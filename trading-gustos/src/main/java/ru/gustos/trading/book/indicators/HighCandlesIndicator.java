package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class HighCandlesIndicator implements IIndicator{
    public static final int Id = 40;

    IndicatorPeriod period;
    boolean positive;

    public HighCandlesIndicator(IndicatorPeriod period, boolean positive){
        this.period = period;
        this.positive = positive;
    }

    @Override
    public int getId() {
        return (positive?Id:Id+10) + period.ordinal();
    }

    @Override
    public String getName() {
        return (positive?"Pos":"Neg")+"Candles_"+period.name();
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        int bars = IndicatorUtils.bars(period,sheet);
        double[] candles = new double[values.length];

        for (int i = 0; i < values.length; i++)
            candles[i] = Math.abs(sheet.moments.get(i).bar.getClosePrice()-sheet.moments.get(i).bar.getOpenPrice());

        for (int i = 0; i < values.length; i++){
            int from = Math.max(0, i - bars + 1);
            int length = i - from + 1;
            double p = VecUtils.nth(candles, from, length,length*3/4);
            double v = 0;
            for (int j = Math.max(i-bars+1,0);j<=i;j++) {
                double d = sheet.moments.get(j).bar.getClosePrice() - sheet.moments.get(j).bar.getOpenPrice();
                if (positive) {
                    if (d > 5 && d > p * 3)
                        v++;
                } else {
                    if (d < 5 && d < -p * 3)
                        v++;

                }
            }
            values[i] = v;
        }
    }

    @Override
    public Color getColorMax() {
        return positive?Color.green:Color.red;
    }

    public Color getColorMin() {        return Color.white;    }

    @Override
    public boolean fromZero() {
        return true;
    }
}


