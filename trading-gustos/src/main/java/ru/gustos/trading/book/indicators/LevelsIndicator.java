package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

public class LevelsIndicator extends NumberIndicator{
    GustosVolumeLevel2 core;

    public LevelsIndicator(IndicatorInitData data) {
        super(data);
    }

    @Override
    public IndicatorVisualType getVisualType() {
        return IndicatorVisualType.PRICELINE;
    }

    @Override
    public Object getCoreObject() {
        return core;
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        core = new GustosVolumeLevel2(data.k2, data.k1, data.k3);
        values[0][0] = core.feed(sheet.bar(0));
        for (int i = 1;i<sheet.size();i++)
            values[0][i] = core.feed(sheet.bar(i));
    }

}

