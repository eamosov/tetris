package ru.gustos.trading.book;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bot.TradeHistory;
import ru.gustos.trading.book.indicators.Indicator;

import java.io.*;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PlayIndicator{
    public PlayResults playIndicator(Sheet sheet, int indicator, int from, int to) {
        Indicator ii = sheet.getLib().get(indicator);
        double[] v = sheet.getData().get(indicator);
        double buyCost = 0;
        double btc = 0;
        double fee = 0.0005;
        PlayResults result = new PlayResults();
        double bestPrice = 0;
        result.money = new double[v.length];
        double money = 1000;
        Arrays.fill(result.money,money);
        double moneyWhenBuy = 0;
        DecisionStats current = new DecisionStats();
        String buyWhy = null;
        StringBuilder sb = new StringBuilder();

        for (int i = from;i<to;i++){
            XBar bar = sheet.bar(i);
            Map<String, String> marks = ii.getMarks(i);

            if (money>0 && v[i]!=0){
                sb.append("buy "+bar.getBeginTime()+" for "+bar.getClosePrice()+" result "+money+"\n");
                moneyWhenBuy = money;
                bestPrice = buyCost = bar.getClosePrice()*(1+fee);
                btc += money/buyCost;
                money = 0;
                buyWhy = marks==null?"":marks.toString();
            }
            if (btc>0){
                double sellCost = bar.getClosePrice() * (1-fee);
                double min = Double.MAX_VALUE;
                for (int j = -2;j<=2;j++)
                    if (i+j>=0 && i+j<sheet.size())
                        min = Math.min(min,sheet.moments.get(i+j).bar.getMinPrice() * (1-fee));
                bestPrice = Math.max(bestPrice,min);
                if (i==to-1 || v[i]<=0) {
                    sb.append("sell "+bar.getBeginTime()+" for "+bar.getClosePrice()+" result "+btc+"\n");
                    String key = buyWhy + (marks==null?"":marks.toString());
                    money += btc * sellCost;
                    btc = 0;
                    DecisionStats spec;
                    if (result.specific.containsKey(key))
                        spec = result.specific.get(key);
                    else{
                        spec = new DecisionStats();
                        result.specific.put(key,spec);
                    }
                    result.general.profit *= sellCost/buyCost;
                    result.general.trades++;
                    result.general.bestPossibleProfit*=bestPrice/buyCost;
                    spec.profit *= sellCost/buyCost;
                    spec.trades++;
                    spec.bestPossibleProfit*=bestPrice/buyCost;
                    if (sellCost > buyCost) {
                        result.general.profitableCount++;
                        result.general.successProfit += sellCost / buyCost;
                        spec.profitableCount++;
                        spec.successProfit+=sellCost/buyCost;
                    } else {
                        result.general.looseProfit += sellCost / buyCost;
                        spec.looseProfit+=sellCost/buyCost;
                    }
                }
            }
            result.money[i] = money==0?btc*bar.getClosePrice()*0.9995:money;
        }
        if (to<result.money.length)
            Arrays.fill(result.money,to,result.money.length,money);

        result.general.complete();
        result.specific.forEach((s,d)->d.complete());
        result.assetPriceChange = sheet.moments.get(to).bar.getClosePrice()/sheet.moments.get(from).bar.getClosePrice();
        string2file("play.txt",sb.toString());
        return result;
    }

    public static void string2file(String path, String s)  {
        try {
            try (Writer out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(path), "UTF-8"))){
                out.write(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class PlayResults {
        DecisionStats general = new DecisionStats();
        HashMap<String,DecisionStats> specific = new HashMap<>();
        public double[] money;
        public double assetPriceChange;

        public PlayResults() {}

        public PlayResults(Sheet sheet, TradeHistory history) {
            money = new double[sheet.size()];
            if (history.getCash().size()>0) {

                int i = 0;
                double v = history.getStartUsd();
                for (Pair<ZonedDateTime, Float> z : history.getCash()) {
                    double now = v;
                    double next = z.getSecond();
                    int from = i;
                    int to = sheet.getBarIndex(z.getFirst());
                    while (i<to){
                        money[i] = now+(next-now)*(i-from)/(to-from);
                        i++;
                    }
                    v = z.getSecond();
                    if (i>=money.length) break;
                }
                Arrays.fill(money,i,money.length,v);
            }

        }

        public String toString(){
            StringBuilder sb = new StringBuilder();
            sb.append("Profit to grow: ").append(general.profit/assetPriceChange).append("\n");
            sb.append("General:\n");
            String spaces = "    ";
            sb.append(general.toString(spaces)).append("\n");
            for (String key : specific.keySet()){
                sb.append(key+":\n");
                sb.append(specific.get(key).toString(spaces)).append("\n");
            }
            return sb.toString();
        }

    }

    public static class DecisionStats{
        public int trades;
        public double profit = 1;
        public double profitable;
        public double successProfit;
        public double looseProfit;
        public double bestPossibleProfit = 1;
        public int profitableCount;

        public String toString(String spaces){
            String res = String.format("trades: %d\nprofitable %%: %.4g\nprofit: %.4g\nsuccessProfit: %.4g\nlooseProfit: %.4g\nbestPossible: %.4g", trades, profitable, profit, successProfit, looseProfit, bestPossibleProfit);

            return spaces+res.replace("\n","\n"+spaces);
        }

        public void complete() {
            profitable = ((double)profitableCount)/trades;
            successProfit/=Math.max(1,profitableCount);
            looseProfit/=Math.max(1,trades-profitableCount);

        }
    }

}
