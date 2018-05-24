package ru.gustos.trading.book.indicators;

public abstract class NumberIndicator extends Indicator {
    public NumberIndicator(IndicatorInitData data) {
        super(data);
    }

    @Override
    public IndicatorResultType getResultType() {
        return IndicatorResultType.NUMBER;
    }
}
