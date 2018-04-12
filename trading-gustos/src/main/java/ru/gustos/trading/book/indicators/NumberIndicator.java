package ru.gustos.trading.book.indicators;

import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public abstract class NumberIndicator extends BaseIndicator{
    public NumberIndicator(IndicatorInitData data) {
        super(data);
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }

    public Color getColorMin() {        return CandlesPane.RED;    }
}
