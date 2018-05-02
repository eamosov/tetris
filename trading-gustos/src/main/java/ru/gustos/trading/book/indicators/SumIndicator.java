package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;
import java.util.Arrays;

public class SumIndicator extends BaseIndicator{

    int[] ids;
    double[] weights;

    public SumIndicator(IndicatorInitData data){
        super(data);
        String[] s = data.indicators.split(",");
        ids = new int[s.length/2];
        weights = new double[s.length/2];
        for (int i = 0;i<s.length;i+=2){
            ids[i/2] = Integer.parseInt(s[i]);
            weights[i/2] = Double.parseDouble(s[i+1]);
        }
    }

    @Override
    public String getName() {
        return "Sum_"+id;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        Arrays.fill(values,0);
        for (int i = from;i<to;i++)
            for (int j = 0;j<ids.length;j++)
                values[i] +=  sheet.getData().get(ids[j],i)*weights[j];
    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }

    public Color getColorMin() {        return Color.gray;    }

    @Override
    public boolean fromZero() {
        return true;
    }
}
