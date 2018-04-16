package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class BoundOptimizedIndicator extends BaseIndicator{
    private int ind;
    public BoundOptimizedIndicator(IndicatorInitData data){
        super(data);
        ind = data.ind;
    }

    @Override
    public String getName() {
        return "optimized_"+ind;
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

        double[] data = sheet.getData().get(ind);

        boolean buy = false;
        double buyWhenPrice = 0;
        double sellWhenPrice = 0;
        for (int i = 0; i < values.length; i++) {

            double v = data[i];

            XBar bar = sheet.moments.get(i).bar;
            if (v == IIndicator.YES ){
                double price = bar.getMaxPrice();
                if (price>buyWhenPrice || bar.getClosePrice()>bar.getOpenPrice()*1.001) {
                    buy = true;
                    buyWhenPrice = 0;
                } else
                    buyWhenPrice = Math.min(buyWhenPrice,price*1.001);

                sellWhenPrice = 0;
            } else {
                double price = bar.getMinPrice();
                if (price<sellWhenPrice || bar.getClosePrice()*1.001<bar.getOpenPrice()) {
                    buy = false;
                    sellWhenPrice = 10000000;
                } else
                    sellWhenPrice = Math.max(sellWhenPrice,price/1.001);

                buyWhenPrice = 10000000;
            }

            if (buy)
                values[i] =IIndicator.YES;
            else
                values[i] =IIndicator.NO;
        }
    }

}

