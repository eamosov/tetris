package ru.gustos.trading.bots;

import ru.gustos.trading.book.Sheet;

import java.util.ArrayList;

public class BotRunner {
    public static final double startMoney = 1000;

    public static final double fee = 0.9995;

    public ArrayList<BotInterval> intervals = new ArrayList<>();

    public double run(Sheet sheet, int from, int to, IDecisionBot bot){
        double money = startMoney;
        double btc = 0;
        intervals.clear();

        BotInterval cur = null;
        for (int i = from;i<to;i++){
            if (money>0 && bot.shouldBuy(sheet, i)){
                btc += money/sheet.bar(i).getClosePrice()*fee;
                money = 0;
                cur = new BotInterval();
                cur.buyMoment = i;
            } else if (btc>0 && bot.shouldSell(sheet, i)){
                money+=btc*sheet.bar(i).getClosePrice()*fee;
                btc = 0;
                cur.sellMoment = i;
                intervals.add(cur);
            }
        }
        if (btc>0) {
            money += btc * sheet.moments.get(to - 1).bar.getClosePrice() * fee;
            cur.sellMoment = to-1;
            intervals.add(cur);
        }

        return money;
    }
}

