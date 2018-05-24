package ru.gustos.trading.book.ml;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.GustosAverageRecurrent;
import ru.gustos.trading.book.indicators.Indicator;
import ru.gustos.trading.book.indicators.IndicatorResultType;
import ru.gustos.trading.visual.Visualizator;

public class OrderBot {
    static Sheet sheet;
    static double money = 1000;
    static double btc = 0;
    static double buyPrice = 0;

    static double buyOrder = 0;
    static double sellOrder = 0;

    static double fee = 0.0005;
    static int count = 0;
    static int profitable = 0;
    static int buyIndex = 0;
    static double[] g;

    public static void main(String[] args) throws Exception {
        sheet = TestUtils.makeSheet("indicators_simple.json");
//        StringBuilder sb = new StringBuilder("<TICKER>\t<PER>\t<DATE>\t<TIME>\t<CLOSE>\n");
//        for (int i = 0 ;i<sheet.size();i++) {
//            XBar b = sheet.bar(i);
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
        int to = sheet.size();

        double[] v = sheet.moments.stream().mapToDouble(m -> m.bar.getClosePrice()).toArray();
        double[] vols = sheet.moments.stream().mapToDouble(m -> m.bar.getVolume()).toArray();
        int w = 100;
        GustosAverageRecurrent gar = new GustosAverageRecurrent(w,w*4,5);
//        Pair<double[], double[]> pp = VecUtils.gustosMcginleyAndDisp(v, w, vols, w*4);
//        Pair<double[], double[]> pp = VecUtils.gustosMcginleyAndDisp(v, 150, vols, 600);
//        Pair<double[], double[]> pp = VecUtils.emaAndDisp(v, 150);
//        double[] ema = pp.getFirst();
//        double[] disp = pp.getSecond();

        g = new double[v.length];

//        Sd5Logic botLogic = (Sd5Logic) (BotLogic)LogicFactory.Companion.getLogic("sd5",
//                Instrument.Companion.getBTC_USDT(),
//                BarInterval.ONE_MIN,
//                sheet.moments.stream()
//                        .map(m -> new XExtBar(m.bar))
//                        .collect(Collectors.toList()));
//
//        botLogic.loadState("sd3_2017_12_16_04_28.properties");
//        botLogic.prepare();
        Pair<Double, Double> prev = gar.feed(v[0], vols[0]);
        for (int i = from;i<to;i++) {
            Pair<Double, Double> avg = gar.feed(v[i], vols[i]);
            XBar bar = sheet.bar(i);
            if (money > 0) {
                double p = prev.getFirst()-prev.getSecond()*2;
                boolean check = bar.getMinPrice() <= p && bar.getMaxPrice() >= p && !falling(i) && bar.getClosePrice()<avg.getFirst()-avg.getSecond();

                if (check) {
                    buy(i);
                }
            } else if (btc > 0) {
                double p = prev.getFirst()+prev.getSecond()*2;
                boolean check = bar.getMinPrice() <= p && bar.getMaxPrice() >= p && !rising(i) && bar.getClosePrice()>avg.getFirst()+avg.getSecond();

                if (check)
                    sell(i);
            }
            prev = avg;
        }
        System.out.println(String.format("profitable: %.3g, count: %d, pertrade: %.5g", profitable*1.0/count,count,Math.pow(money/1000,1.0/count)));
        sheet.getLib().add("result", IndicatorResultType.YESNO,g);
        sheet.calcIndicators();

        new Visualizator(sheet);

    }

    private static boolean rising(int i) {
        XBar pbar = sheet.moments.get(i-1).bar;
        XBar bar = sheet.bar(i);
        return pbar.getClosePrice()<bar.getMinPrice();
    }

    private static boolean falling(int i) {
        XBar pbar = sheet.moments.get(i-1).bar;
        XBar bar = sheet.bar(i);
        return pbar.getClosePrice()>=bar.getMaxPrice();
    }

    private static void sell(int index) {
        sellOrder = sheet.bar(index).getClosePrice();
        money = btc*sellOrder*(1-fee);
        btc = 0;
        System.out.println(String.format("money: %d, time: %d, profit: %.3g%%", (int)money,index-buyIndex,(sellOrder*(1-fee)/buyPrice-1)*100));
        boolean good = sellOrder * (1 - fee) / buyPrice > 1;
        if (good)
            profitable++;
        for (int j = buyIndex;j<=index;j++)
            g[j] = good? Indicator.YES: Indicator.NO;
        sellOrder = 0;
    }

    private static void buy(int index) {
        buyOrder = sheet.bar(index).getClosePrice();
        btc = money / buyOrder * (1 - fee);
        money = 0;
        buyPrice = buyOrder * (1 + fee);
        count++;
        buyIndex = index;
        buyOrder = 0;

    }
}
