package ru.gustos.trading.bots;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.Decision;

import java.util.Random;

public class RandomBot implements  IDecisionBot{
    Random r = new Random();

    @Override
    public boolean shouldBuy(Sheet sheet, int index) {
        return r.nextInt(50)==0;
    }

    @Override
    public boolean shouldSell(Sheet sheet, int index) {
        return r.nextInt(50)==0;
    }
}
