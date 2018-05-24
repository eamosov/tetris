package ru.gustos.trading.book.indicators;

import ru.efreet.trading.logic.impl.sd5.Sd5Logic;
import ru.gustos.trading.book.Moment;
import ru.gustos.trading.book.Sheet;

public class ShouldSellIndicator extends Indicator {

    public ShouldSellIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        EfreetIndicator indicator = (EfreetIndicator)sheet.getLib().get(EfreetIndicator.Id);
        Sd5Logic botLogic = (Sd5Logic)indicator.botLogic;
        int lookNext = 30;
        double s = 0;
        int c = 0;
        for (int i = from;i<to-lookNext;i++) if (botLogic.shouldSell(i)){
            Moment moment = sheet.moments.get(i);
            double cost = moment.bar.getClosePrice();
            double best = cost;
            for (int j = i+1;j<i+lookNext;j++){
                if (botLogic.shouldSell(j)) {
                    best = Math.max(sheet.moments.get(j).bar.getClosePrice(),best);
                }
            }
            if (best>cost) {
                values[0][i] = Indicator.NO;
                double w = (best / cost - 1)*100;
                moment.weight = w;
                s+=w;
                c++;
//                values[i] = -moment.weight;
            } else {
                values[0][i] = Indicator.YES;
                moment.weight = 1;
//                values[i] = moment.weight;
            }

        }
//        System.out.println(s/c);
    }

}
