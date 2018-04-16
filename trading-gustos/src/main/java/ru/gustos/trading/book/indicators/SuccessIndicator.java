package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class SuccessIndicator extends BaseIndicator{
    private int ind;
    public SuccessIndicator(IndicatorInitData data){
        super(data);
        ind = data.ind;
    }

    @Override
    public String getName() {
        return "success_"+ind;
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
    public void calcValues(Sheet sheet, double[] values) {

        double[] data = sheet.getData().get(ind);

        double buyPrice = 0;
        int buyPos = 0;
        for (int i = 0; i < values.length; i++) {

            double v = data[i];

            XBar bar = sheet.moments.get(i).bar;
            if (v == IIndicator.YES ){
                if (buyPos==0) {
                    buyPrice = bar.getClosePrice();
                    buyPos = i;
                }
            } else if (buyPos>0){
                double price = bar.getClosePrice();
                double result = price/buyPrice>1.001?IIndicator.YES:IIndicator.NO;
                for (int j = buyPos;j<=i;j++)
                    values[j] = result;
                buyPos = 0;
            }
        }
    }

}
