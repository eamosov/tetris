package ru.gustos.trading.bots;

import ru.gustos.trading.book.Sheet;

public interface IDecisionBot {

    boolean shouldBuy(Sheet sheet, int index);

    boolean shouldSell(Sheet sheet, int index);
}

