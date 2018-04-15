package ru.gustos.trading.book.indicators;

import ru.efreet.trading.Decision;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class TargetDecisionIndicator implements IIndicator{
    public static final int Id = 1;

    @Override
    public int getId() {
        return Id;
    }

    @Override
    public String getName() {
        return "Decision";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        for (int i = 0;i<sheet.moments.size();i++) {
            Decision d = sheet.moments.get(i).decision;
            values[i] = d ==Decision.BUY?IIndicator.YES:(d==Decision.SELL?IIndicator.NO:0);
        }

    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }

    @Override
    public Color getColorMin() {
        return CandlesPane.RED;
    }
}
