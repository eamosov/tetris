package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class SignChangeIndicator extends BaseIndicator {
    int ind;
    int period;
    boolean toPlus;

    public SignChangeIndicator(IndicatorInitData data){
        super(data);
        ind = data.ind;
        period =  data.t1;
        toPlus = data.positive;
    }

    @Override
    public String getName() {
        return "sign_"+ind+"_"+period+"_"+(toPlus?"pos":"neg");
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }

    @Override
    public Color getColorMin() {
        return CandlesPane.RED;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        int bars = period*5;
        double[] vv = sheet.getData().get(ind);
        for (int i = bars;i<sheet.moments.size();i++){
            double v = vv[i];
            if (toPlus)
                values[i] = (v > 0 && vv[i-bars]<0)?IIndicator.YES:IIndicator.NO;
            else
                values[i] = (v < 0 && vv[i-bars]>0)?IIndicator.YES:IIndicator.NO;
        }


    }
}
