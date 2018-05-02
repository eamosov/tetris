package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.logic.BotLogic;
import ru.efreet.trading.logic.impl.sd3.Sd3Logic;
import ru.efreet.trading.logic.impl.sd5.Sd5Logic;
import ru.gustos.trading.book.Moment;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

import static ru.gustos.trading.book.SheetUtils.sellValues;

public class ShouldBuyIndicator extends BaseIndicator {

    public ShouldBuyIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public String getName() {
        return "ShouldBuy";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        EfreetIndicator indicator = (EfreetIndicator)sheet.getLib().get(EfreetIndicator.Id);
        Sd5Logic botLogic = (Sd5Logic)indicator.botLogic;
        int lookNext = 240;
        for (int i = from;i<to-lookNext;i++) {
            double bestprofit = 1;
            for (int j = i+1;j<i+lookNext;j++){
                Moment moment = sheet.moments.get(i);
                double profit = sheet.moments.get(j).bar.getClosePrice()/ moment.bar.getClosePrice();
                bestprofit = Math.max(bestprofit,profit);
                if (botLogic.shouldSell(j)) {
                    values[i] = profit>1.002?IIndicator.YES:0;
                    moment.weight = profit>=1?(profit-1):(1/profit-1)*2*(bestprofit/profit);
                    break;
                }
            }
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

