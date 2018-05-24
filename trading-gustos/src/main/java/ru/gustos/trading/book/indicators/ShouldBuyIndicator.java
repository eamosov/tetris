package ru.gustos.trading.book.indicators;

import ru.efreet.trading.logic.impl.sd5.Sd5Logic;
import ru.gustos.trading.book.Moment;
import ru.gustos.trading.book.Sheet;

public class ShouldBuyIndicator extends Indicator {

    public ShouldBuyIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        EfreetIndicator indicator = (EfreetIndicator)sheet.getLib().get(EfreetIndicator.Id);
        Sd5Logic botLogic = (Sd5Logic)indicator.botLogic;
        int lookNext = 240;
        for (int i = from;i<to-lookNext;i++) {
            Moment moment = sheet.moments.get(i);
            double close = moment.bar.getClosePrice();
            double min = close;
            int better = 0;
            for (int j = i+1;j<i+lookNext;j++){
                double p = sheet.moments.get(j).bar.getClosePrice();
                double profit = p / close;
                if (p<close)
                    better++;
                min = Math.min(min,p);
                if (botLogic.shouldSell(j)) {
                    double bestprofit = p/min;

                    if (profit<1.002 || better>(j-i)/2+5){
                        values[0][i] = 0;
                        double ww = (bestprofit/profit-1);
                        if (profit<1.002) {
                            double w = (1 / (profit / 1.002)) - 1;
                            moment.weight = Math.max(ww*100,w*100);
                        } else
                            moment.weight = ww*100;
//                        values[i] = -moment.weight;

                    } else {
                        values[0][i] = Indicator.YES;
                        moment.weight = (profit-1)*100;
//                        values[i] = moment.weight;
                    }
                    moment.weight = Math.pow(moment.weight,1.5);
                    break;
                }
            }
        }
    }

}

