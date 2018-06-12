package ru.gustos.trading.book.indicators;

import ru.efreet.trading.Decision;
import ru.efreet.trading.bot.TradeHistory;
import ru.efreet.trading.exchange.TradeRecord;
import ru.gustos.trading.book.Sheet;

import java.util.Arrays;

public class TradeHistoryTradesIndicator extends Indicator {
    TradeHistory history;
    public TradeHistoryTradesIndicator(int idd, TradeHistory history){
        super(new IndicatorInitData(){{id = idd;show = false;showOnBottom = false;}});
        this.history = history;
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {

        try {
            double v = 0;
            int i = from;
            for (TradeRecord tr : history.getTrades()){
                while (i<to && tr.getTime().isAfter(sheet.bar(i).getEndTime())){
                    values[0][i] = v;
                    i++;
                }
                v = tr.getDecision()== Decision.BUY? Indicator.YES: Indicator.NO;
                if (i>=to) break;
            }
            if (i<to)
                Arrays.fill(values[0],i,to,v);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
