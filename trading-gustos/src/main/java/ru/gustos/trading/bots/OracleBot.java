package ru.gustos.trading.bots;

import ru.gustos.trading.book.Sheet;
import ru.efreet.trading.Decision;

public class OracleBot implements  IDecisionBot{

    @Override
    public boolean shouldBuy(Sheet sheet, int index) {
        return sheet.moments.get(index).decision== Decision.BUY;
    }

    @Override
    public boolean shouldSell(Sheet sheet, int index) {
        return sheet.moments.get(index).decision== Decision.SELL;
    }
}

