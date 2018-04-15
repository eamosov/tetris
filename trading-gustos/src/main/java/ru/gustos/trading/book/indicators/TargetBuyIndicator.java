package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

import static ru.gustos.trading.book.SheetUtils.sellValues;

public class TargetBuyIndicator extends BaseIndicator {
    public static int Id;

    public TargetBuyIndicator(IndicatorInitData data){
        super(data);
        Id = data.id;
    }

    @Override
    public String getName() {
        return "Buy";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        double[] sellValuesPos = sellValues(sheet, true);
        int lookNext = 60;
        for (int i = 0;i<sheet.moments.size()-lookNext;i++) {
            XBar bar = sheet.moments.get(i).bar;
            int pos = 0;
            for (int j = i+1;j<i+lookNext;j++){
                if (bar.getClosePrice()*1.0075<sellValuesPos[j])
                    pos++;
            }

            values[i] = pos>3?IIndicator.YES:0;
        }
    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }

    @Override
    public Color getColorMin() {
        return Color.darkGray;
    }
}


