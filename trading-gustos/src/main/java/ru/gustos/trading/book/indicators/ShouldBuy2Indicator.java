package ru.gustos.trading.book.indicators;

import ru.efreet.trading.logic.impl.sd5.Sd5Logic;
import ru.gustos.trading.book.Moment;
import ru.gustos.trading.book.Sheet;

public class ShouldBuy2Indicator extends Indicator {

    public ShouldBuy2Indicator(IndicatorInitData data){
        super(data);
    }


    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        EfreetIndicator indicator = (EfreetIndicator)sheet.getLib().get(EfreetIndicator.Id);
        Sd5Logic botLogic = (Sd5Logic)indicator.botLogic;
        int lookNext = 240;
        for (int i = from;i<to-lookNext;i++) {
            double bestprofit = 0;
            double min = sheet.bar(i).getClosePrice();
            for (int j = i+1;j<i+lookNext;j++){
                Moment moment = sheet.moments.get(i);
                double p = sheet.moments.get(j).bar.getClosePrice();
                double profit = p / moment.bar.getClosePrice();
                if (p<min) min = p;
                bestprofit = Math.max(bestprofit,p/min);
                if (botLogic.shouldSell(j)) {
//                    values[i] = profit>=1.002 ?Indicator.YES:0;
                    values[0][i] = profit>=1.002 ? Indicator.YES:0;
                    moment.weight = (profit>=1?(profit-1):(1/profit-1));//*(profit/bestprofit);
                    break;
                }
            }
        }
    }

    @Override
    public ColorScheme getColors() {
        return ColorScheme.GREENGRAY;
    }

}
