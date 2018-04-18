package ru.gustos.trading.bots;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.EfreetSd3Indicator;

public class EfreetBot implements  IDecisionBot{

    @Override
    public boolean shouldBuy(Sheet sheet, int index) {
        return sheet.getData().get(EfreetSd3Indicator.Id, index)>0;
    }

    @Override
    public boolean shouldSell(Sheet sheet, int index) {
        return sheet.getData().get(EfreetSd3Indicator.Id, index)<0;
    }
}

