package ru.gustos.trading.bots;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.Decision;
import ru.gustos.trading.book.indicators.EfreetIndicator;

public class EfreetBot implements  IDecisionBot{

    @Override
    public boolean shouldBuy(Sheet sheet, int index) {
        return sheet.getData().get(EfreetIndicator.Id,index)>0;
    }

    @Override
    public boolean shouldSell(Sheet sheet, int index) {
        return sheet.getData().get(EfreetIndicator.Id,index)<0;
    }
}

