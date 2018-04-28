package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.logic.BotLogic;
import ru.efreet.trading.logic.impl.sd3.Sd3Logic;
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
    public void calcValues(Sheet sheet, double[] values) {
        EfreetIndicator indicator = (EfreetIndicator)sheet.getLib().get(EfreetIndicator.Id);
        Sd3Logic botLogic = (Sd3Logic)indicator.botLogic;
        int lookNext = 240;
        for (int i = 0;i<sheet.moments.size()-lookNext;i++) {
            for (int j = i+1;j<i+lookNext;j++){
                if (botLogic.shouldSell(j)) {
                    double profit = sheet.moments.get(j).bar.getClosePrice()/sheet.moments.get(i).bar.getClosePrice();
                    values[i] = profit>1.002?IIndicator.YES:0;
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

