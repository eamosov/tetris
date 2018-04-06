package ru.gustos.trading.bots;

import ru.gustos.trading.book.Sheet;

import java.util.Arrays;

public class BotRunner {
    public static final double startMoney = 1000;

    public static final double fee = 0.9975;

    public double[] run(Sheet sheet, int from, int to, IDecisionBot bot){
        double[] res = new double[sheet.moments.size()];
        double money = startMoney;
        double btc = 0;
        if (from>0)
            Arrays.fill(res,0,from,money);

        for (int i = from;i<to;i++){
            if (money>0 && bot.shouldBuy(sheet, i)){
                btc += money/sheet.moments.get(i).bar.getClosePrice()*fee;
                money = 0;
            } else if (btc>0 && bot.shouldSell(sheet, i)){
                money+=btc*sheet.moments.get(i).bar.getClosePrice()*fee;
                btc = 0;
            }
            res[i] = money;
        }
        if (btc>0) {
            money += btc * sheet.moments.get(to - 1).bar.getClosePrice() * fee;
            res[to-1] = money;
        }
        if (to<res.length)
            Arrays.fill(res,to,res.length,money);

        return res;
    }
}

