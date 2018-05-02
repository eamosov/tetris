package ru.gustos.trading.bots;

import kotlin.reflect.KClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.bot.BotAdvice;
import ru.efreet.trading.bot.Trader;
import ru.efreet.trading.bot.TradesStats;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.logic.AbstractBotLogic;
import ru.efreet.trading.ta.indicators.XIndicator;
import ru.efreet.trading.trainer.Metrica;
import ru.gustos.trading.book.Sheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                btc += money/sheet.moments.get(i).bar.getClosePrice()*fee;
                money = 0;
                cur = new BotInterval();
                cur.buyMoment = i;
            } else if (btc>0 && bot.shouldSell(sheet, i)){
                money+=btc*sheet.moments.get(i).bar.getClosePrice()*fee;
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

