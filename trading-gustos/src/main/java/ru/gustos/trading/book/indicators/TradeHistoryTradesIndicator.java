package ru.gustos.trading.book.indicators;

import ru.efreet.trading.Decision;
import ru.efreet.trading.bot.TradeHistory;
import ru.efreet.trading.exchange.TradeRecord;
import ru.gustos.trading.book.Sheet;

import java.awt.*;
import java.util.Arrays;

public class TradeHistoryTradesIndicator extends BaseIndicator{
    TradeHistory history;
    public TradeHistoryTradesIndicator(int id, TradeHistory history){
        super(id);
        this.history = history;
        show = false;
    }

    @Override
    public String getName() {
        return "trades";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public Color getColorMax() {
        return Color.green;
    }

    @Override
    public Color getColorMin() {
        return Color.red;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {

        try {
            double v = 0;
            int i = from;
            for (TradeRecord tr : history.getTrades()){
                while (i<to && tr.getTime().isAfter(sheet.moments.get(i).bar.getEndTime())){
                    values[i] = v;
                    i++;
                }
                v = tr.getDecision()== Decision.BUY?IIndicator.YES:IIndicator.NO;
                if (i>=to) break;
            }
            if (i<to)
                Arrays.fill(values,i,to,v);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
