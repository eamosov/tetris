package ru.gustos.trading.book.indicators;

import ru.efreet.trading.logic.impl.sd3.Sd3Logic;
import ru.efreet.trading.logic.impl.sd5.Sd5Logic;
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
    public void calcValues(Sheet sheet, double[] values, int from, int to) {

        double[] d = sheet.getData().get(ind);
        Sd5Logic sd3 = (Sd5Logic)((EfreetIndicator)sheet.getLib().get(1)).botLogic;

        try {

            boolean buy = false;
            boolean stopBuy = false;
            for (int i = 0;i<to;i++) {
                if (buy){
                    if (sd3.shouldSell(i)) {
                        buy = false;
                        stopBuy = true;
                    }
                } else {
                    if (d[i]>0 && !stopBuy)
                        buy = true;
                    if (d[i]<=0)
                        stopBuy = false;
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
