package ru.gustos.trading.bots;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.Decision;

public class CheckPeriodBot implements  IDecisionBot{
    int period;
    int sell;
    boolean winner;

    public CheckPeriodBot(int period, boolean winner){
        this.period = period;
        this.winner = winner;
    }

    public void reset(){
        sell = -1;
    }

    @Override
    public boolean shouldBuy(Sheet sheet, int index) {
        if (index+period+5>=sheet.moments.size()) return false;
        double now = sheet.moments.get(index).bar.getClosePrice();
        int bestpos = index+period+5;
        double bestp = sheet.moments.get(bestpos).bar.getClosePrice();
        for (int i = -5;i<5;i++){
            double p = sheet.moments.get(index+period+i).bar.getClosePrice();
            if ((winner && p<bestp) || (!winner && p>bestp)){
                bestp = p;
                bestpos = index+period+i;
            }
        }

        if (winner && bestp>now*1.03) {
            sell = bestpos;
            return true;
        }

        if (!winner && bestp<now*0.97) {
            sell = bestpos;
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldSell(Sheet sheet, int index) {
        return index==sell;
    }
}
