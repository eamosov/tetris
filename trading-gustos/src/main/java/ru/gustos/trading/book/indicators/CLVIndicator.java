package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

public class CLVIndicator extends NumberIndicator{

    public CLVIndicator(IndicatorInitData data) {
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        EmaRecurrent ema = new EmaRecurrent(3000);
        for (int i = 0;i<sheet.size();i++) {
            XBar bar = sheet.bar(i);
            double d = bar.getMaxPrice() - bar.getMinPrice();
            if (d==0) d = 1000000;
            double v = (i==0?0:values[0][i - 1]) + ((bar.getClosePrice() - bar.getMinPrice()) - (bar.getMaxPrice() - bar.getClosePrice())) / d * bar.getVolume();
            double r = ema.feed(v);
            values[0][i] = v-r;
        }
    }

}
