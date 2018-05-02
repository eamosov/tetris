package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;
import java.util.Arrays;

public class FileIndicator extends BaseIndicator{
    private String state;
    public FileIndicator(IndicatorInitData data){
        super(data);
        state = data.state;
    }

    @Override
    public String getName() {
        return "file_"+ state;
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
    public void calcValues(Sheet sheet, double[] values, int from, int to) {

        try {
            double[] v = VecUtils.fromFile("d:/tetrislibs/agents/"+state);
            System.arraycopy(v,from,values,from,to-from);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
