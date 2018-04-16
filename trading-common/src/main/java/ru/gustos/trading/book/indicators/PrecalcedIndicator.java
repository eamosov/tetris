package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class PrecalcedIndicator implements IIndicator{
    int id;
    String name;
    IndicatorType type;
    double[] data;

    public PrecalcedIndicator(int id, String name, IndicatorType type, double[] data){
        this.id = id;
        this.name = name;
        this.type = type;
        this.data = data;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IndicatorType getType() {
        return type;
    }

    @Override
    public boolean fromZero() {
        return false;
    }

    @Override
    public boolean showOnPane() {
        return true;
    }

    @Override
    public Color getColorMax() { return CandlesPane.GREEN; }

    @Override
    public Color getColorMin() {
        return CandlesPane.RED;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        System.arraycopy(data,0,values,0,Math.min(data.length,values.length));
    }
}
