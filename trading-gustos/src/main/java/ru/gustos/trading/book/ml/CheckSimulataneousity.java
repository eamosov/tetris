package ru.gustos.trading.book.ml;

import com.google.gson.Gson;
import kotlin.Pair;
import org.apache.commons.io.FileUtils;
import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bot.TradeHistory;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.TradeRecord;
import ru.efreet.trading.logic.ProfitCalculator;
import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.Sheet;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

public class CheckSimulataneousity{
    static Sheet sheet;

    public static void main(String[] args) throws Exception{
        sheet = TestUtils.makeSheet("indicators_simple.json", Instrument.getBTC_USDT());

        Hashtable<ZonedDateTime,Integer> buys = new Hashtable<>();
        Hashtable<ZonedDateTime,Integer> sells = new Hashtable<>();
        Hashtable<ZonedDateTime,Pair<Double,Integer>> profitsBuy = new Hashtable<>();
        Hashtable<ZonedDateTime,Pair<Double,Integer>> profitsSell = new Hashtable<>();
        Hashtable<Integer,Pair<Double,Integer>> totalBuy = new Hashtable<>();
        Hashtable<Integer,Pair<Double,Integer>> totalSell = new Hashtable<>();
        String path = "gustoslogic2.properties.results2";
        ResultsToWeka.TM[] initData = new Gson().fromJson(FileUtils.readFileToString(new File(path)),ResultsToWeka.TM[].class);
        ArrayList<TradeRecord[]> records = new ArrayList<>();
        for (int i = initData.length*9/10;i<initData.length;i++){
            BarInterval barInterval = BarInterval.ONE_MIN;
            String logic = "gustos2";
            ArrayList<Pair<ZonedDateTime,ZonedDateTime>> aa = new ArrayList<>();
            ZonedDateTime t1,t2;
                t1 = ZonedDateTime.of(2018, 4, 15, 0, 0, 0, 0, ZoneId.systemDefault());
                t2 = ZonedDateTime.of(2018, 5, 15, 0, 0, 0, 0, ZoneId.systemDefault());
            aa.add(new Pair<>(t1, t2));
            List<XBar> bars = sheet.moments.stream()
                    .map(m -> m.bar)
                    .collect(Collectors.toList());
            TradeHistory history = new ProfitCalculator().tradeHistory(logic, initData[i].args, sheet.instrument(), barInterval, sheet.exchange().getFee(), bars, aa, false);
            List<TradeRecord> trades = history.getTrades();
            records.add(trades.toArray(new TradeRecord[0]));
            for (int j = 1;j<trades.size()-1;j++) {
                TradeRecord tr = trades.get(j);
                Hashtable<ZonedDateTime, Integer> h = tr.getDecision()== Decision.BUY?buys:sells;
                Hashtable<ZonedDateTime, Pair<Double, Integer>> p = tr.getDecision() == Decision.BUY ? profitsBuy : profitsSell;

                double profit = tr.getDecision()==Decision.BUY?trades.get(j+1).getPrice()/tr.getPrice():tr.getPrice()/trades.get(j+1).getPrice();
                ZonedDateTime key = tr.getTime();
                h.put(key, h.getOrDefault(key,0)+1);
                Pair<Double, Integer> pp = p.getOrDefault(key, new Pair<>(0.0, 0));
                pp = new Pair<>(pp.getFirst()+profit,pp.getSecond()+1);
                p.put(key,pp);
            }
        }
//        int best = 0;
//        double profit = 0;
//        for (TradeRecord[] t : records){
//            int cc = 0;
//            for (TradeRecord tr : t){
//                Hashtable<ZonedDateTime, Integer> h = tr.getDecision()== Decision.BUY?buys:sells;
//                if (h.get(tr.getTime())>1)
//                    cc++;
//            }
//        }

        complete(buys,profitsBuy);
        complete(sells,profitsSell);
    }

    private static void complete(Hashtable<ZonedDateTime, Integer> buys, Hashtable<ZonedDateTime, Pair<Double, Integer>> profits) {
        Hashtable<Integer,Pair<Double, Integer>> r = new Hashtable<>();
        for (ZonedDateTime z : buys.keySet()){
            int cnt = buys.get(z);
            cnt = (cnt+98)/100;
            Pair<Double, Integer> p = r.getOrDefault(cnt, new Pair<>(0.0, 0));
            Pair<Double, Integer> pp = profits.get(z);
            p = new Pair<>(p.getFirst()+pp.getFirst(),p.getSecond()+pp.getSecond());
            r.put(cnt,p);
        }
        int[] aa = r.keySet().stream().mapToInt(v -> (int) v).toArray();
        Arrays.sort(aa);
        Pair<Double, Integer> p0 = r.get(aa[0]);
        double pr0 = p0.getFirst() / p0.getSecond();
        for (int i = 0;i<aa.length;i++){
            Pair<Double, Integer> p = r.get(aa[i]);
            double pr = p.getFirst() / p.getSecond();
            System.out.println(aa[i]+" "+ (pr/pr0-1)*100 +" "+p.getSecond());
        }
    }

}
