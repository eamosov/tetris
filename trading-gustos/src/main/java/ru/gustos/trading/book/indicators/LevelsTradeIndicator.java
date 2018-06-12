package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

public class LevelsTradeIndicator extends Indicator{

    public LevelsTradeIndicator(IndicatorInitData data) {
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        GustosVolumeLevel v = new GustosVolumeLevel(data.k2, data.k1, data.k3);
        LevelsTrader t = new LevelsTrader(30,100,0.001);
        double level = v.feed(sheet.bar(0));
        t.feed(sheet.bar(0),level);
        for (int i = 1;i<sheet.size();i++) {
            level = v.feed(sheet.bar(i));
            t.feed(sheet.bar(i),level);
            values[0][i] = t.high()?1:0;
        }
    }

}
