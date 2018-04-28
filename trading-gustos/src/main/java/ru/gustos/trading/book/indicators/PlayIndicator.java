package ru.gustos.trading.book.indicators;

import ru.efreet.trading.logic.impl.sd3.Sd3Logic;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class PlayIndicator extends BaseIndicator{
    int ind;
    public PlayIndicator(IndicatorInitData data){
        super(data);
        ind = data.ind;
    }

    @Override
    public String getName() {
        return "play_"+ind;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {

        double[] d = sheet.getData().get(ind);
        Sd3Logic sd3 = (Sd3Logic)((EfreetIndicator)sheet.getLib().get(1)).botLogic;

        try {

            boolean buy = false;
            for (int i = 0;i<d.length;i++) {
                if (buy){
                    if (sd3.shouldSell(i))
                        buy = false;
                } else {
                    if (d[i]>0)
                        buy = true;
                }
                values[i] = buy ? IIndicator.YES : 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
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
