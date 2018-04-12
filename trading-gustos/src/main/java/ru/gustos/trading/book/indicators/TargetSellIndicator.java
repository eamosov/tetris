package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

import static ru.gustos.trading.book.SheetUtils.sellValues;

public class TargetSellIndicator extends BaseIndicator{
    public static int Id;

    public TargetSellIndicator(IndicatorInitData data){
        super(data);
        Id = data.id;
    }

    @Override
    public String getName() {
        return "Sell";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        double[] sellValues = sellValues(sheet, false);
        int lookNext = 60;
        for (int i = 0;i<sheet.moments.size()-lookNext;i++) {
            XBar bar = sheet.moments.get(i).bar;
            int cnt = 0;
            for (int j = i+1;j<i+lookNext;j++){
                if (bar.getClosePrice()*0.995>sellValues[j])
                    cnt++;
            }

            values[i] = cnt>40?IIndicator.YES:0;
        }
    }

    @Override
    public Color getColorMax() {
        return CandlesPane.RED;
    }

    public Color getColorMin() {
        return Color.darkGray;
    }

}

