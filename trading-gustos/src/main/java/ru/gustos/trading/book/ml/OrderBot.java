package ru.gustos.trading.book.ml;

import com.sun.org.apache.bcel.internal.generic.IINC;
import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.logic.BotLogic;
import ru.efreet.trading.logic.impl.LogicFactory;
import ru.efreet.trading.logic.impl.sd5.Sd5Logic;
import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.IIndicator;
import ru.gustos.trading.book.indicators.IndicatorType;
import ru.gustos.trading.book.indicators.VecUtils;
import ru.gustos.trading.visual.Visualizator;

import java.time.ZonedDateTime;
import java.util.stream.Collectors;

public class OrderBot {
    static Sheet sheet;

    public static void main(String[] args) throws Exception {
        Sheet sheet = TestUtils.makeSheetEmptyLib(0);
//        StringBuilder sb = new StringBuilder("<TICKER>\t<PER>\t<DATE>\t<TIME>\t<CLOSE>\n");
//        for (int i = 0 ;i<sheet.moments.size();i++) {
//            XBar b = sheet.moments.get(i).bar;
//            ZonedDateTime t = b.getBeginTime();
//            int n1 = t.getYear()*10000+t.getMonthValue()*100+t.getDayOfMonth();
//            String n2 = Integer.toString(t.getHour()*10000+t.getMinute()*100+t.getSecond());
//            while (n2.length()<6) n2 = "0"+n2;
//            sb.append("BTC\t1\t").append(n1).append('\t').append(n2).append('\t').append(b.getClosePrice()).append('\n');
//        }
//        Exporter.string2file("d:\\tetrislibs\\btc_prices.tdf",sb.toString());
//        if (true) return;

//        int from = 12000;
        int from = 1;
        int to = sheet.moments.size();
        double money = 1000;
        double btc = 0;
        int indicator = 73;
        int sellIndicator = 73;
        double buyPrice = 0;
        double maxprice = 0;
        double maxdema = 0;

        double buyOrder = 0;
        double sellOrder = 0;

        double fee = 0.0005;

        double[] v = sheet.moments.stream().mapToDouble(m -> m.bar.middlePrice()).toArray();
        double[] vols = sheet.moments.stream().mapToDouble(m -> m.bar.getVolume()).toArray();
        Pair<double[], double[]> pp = VecUtils.gustosMcginleyAndDisp(v, 150, vols, 600);
//        Pair<double[], double[]> pp = VecUtils.emaAndDisp(v, 150);
        double[] ema = pp.getFirst();
        double[] disp = pp.getSecond();

        double[] g = new double[v.length];

        Sd5Logic botLogic = (Sd5Logic) (BotLogic)LogicFactory.Companion.getLogic("sd5",
                Instrument.Companion.getBTC_USDT(),
                BarInterval.ONE_MIN,
                sheet.moments.stream()
                        .map(m -> new XExtBar(m.bar))
                        .collect(Collectors.toList()));

        botLogic.loadState("sd3_2018_01_16_04_28.properties");
        botLogic.prepare();

        int count = 0;
        int profitable = 0;
        int buyIndex = 0;
        for (int i = from;i<to;i++){
            XBar bar = sheet.moments.get(i).bar;
            double close = bar.getClosePrice();
            if (money>0){
                boolean check = bar.getMinPrice() <= buyOrder && bar.getMaxPrice() >= buyOrder;
                if (!check && close <ema[i]-disp[i]*2)
                    buyOrder = close;
                if (check){
//                    buyOrder = bar.getClosePrice();
                    btc = money/buyOrder*(1-fee);
                    money = 0;
                    buyPrice = buyOrder*(1+fee);
                    count++;
                    buyIndex = i;
                    buyOrder = 0;
                } else
                    buyOrder = ema[i]-disp[i]*2;
            } else if (btc>0){
                boolean check = bar.getMinPrice() <= sellOrder && bar.getMaxPrice() >= sellOrder;
//                if (!check && close >ema[i]+disp[i]*2)
//                    sellOrder = close;

                if (check){
//                    sellOrder = bar.getClosePrice();
                    money = btc*sellOrder*(1-fee);
                    btc = 0;
                    System.out.println(String.format("money: %d, time: %d", (int)money,i-buyIndex));
                    boolean good = sellOrder * (1 - fee) / buyPrice > 1;
                    if (good)
                        profitable++;
                    for (int j = buyIndex;j<i;j++)
                        g[j] = good? IIndicator.YES: IIndicator.NO;
                    sellOrder = 0;
                } else {
                    sellOrder = ema[i]+disp[i]*2;
                    double upperPrice = botLogic.upperBound(i);
//                    if (bar.getClosePrice()>upperPrice)
//                        sellOrder = (bar.getClosePrice() + upperPrice)/2;
//                    else
//                        sellOrder = upperPrice;

                }
            }
        }
        System.out.println(String.format("profitable: %.3g, count: %d", profitable*1.0/count,count));
        sheet.getLib().add("result", IndicatorType.YESNO,g);
        sheet.calcIndicators();

        new Visualizator(sheet);

    }
}
