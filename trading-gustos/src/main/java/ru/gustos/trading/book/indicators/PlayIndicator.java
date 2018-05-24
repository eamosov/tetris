package ru.gustos.trading.book.indicators;

import ru.efreet.trading.logic.impl.sd5.Sd5Logic;
import ru.gustos.trading.book.Sheet;

public class PlayIndicator extends Indicator {
    public PlayIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public IndicatorResultType getResultType() {
        return IndicatorResultType.YESNO;
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {

        double[] d = sheet.getData().get(data.ind);
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
                values[0][i] = buy ? Indicator.YES : 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public ColorScheme getColors() {
        return ColorScheme.GREENGRAY;
    }
}
