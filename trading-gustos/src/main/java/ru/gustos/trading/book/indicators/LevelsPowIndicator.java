package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

public class LevelsPowIndicator extends NumberIndicator{

    public LevelsPowIndicator(IndicatorInitData data) {
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        GustosVolumeLevel2 v = new GustosVolumeLevel2(data.k2, data.k1, data.k3);
        values[0][0] = v.feed(sheet.bar(0));
        for (int i = 1;i<sheet.size();i++) {
            v.feed(sheet.bar(i));
            values[0][i] = -v.priceForce();
        }
    }

}

