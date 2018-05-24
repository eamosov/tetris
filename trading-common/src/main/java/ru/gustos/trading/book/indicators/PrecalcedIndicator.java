package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class PrecalcedIndicator extends Indicator {
    IndicatorResultType type;
    double[] data;

    public PrecalcedIndicator(int id, String name, IndicatorResultType type, double[] data){
        super(new IndicatorInitData(){{this.id = id;this.name = name;}});
        this.type = type;
        this.data = data;
    }

    @Override
    public IndicatorResultType getResultType() {
        return type;
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        System.arraycopy(data,from,values[0],from,to-from);
    }
}
