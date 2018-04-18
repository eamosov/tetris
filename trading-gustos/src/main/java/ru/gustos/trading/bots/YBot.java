package ru.gustos.trading.bots;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.PredictBuyIndicator;
import ru.gustos.trading.book.indicators.PredictSellIndicator;

public class YBot implements  IDecisionBot{

    @Override
    public boolean shouldBuy(Sheet sheet, int index) {
        return sheet.getData().get(PredictBuyIndicator.Id,index)!=0;
    }

    @Override
    public boolean shouldSell(Sheet sheet, int index) {
        return sheet.getData().get(PredictSellIndicator.Id,index)!=0;
    }
}
