package ru.gustos.trading.book.ml;

import com.sun.org.apache.bcel.internal.generic.IINC;
import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.IIndicator;
import ru.gustos.trading.book.indicators.IndicatorType;
import ru.gustos.trading.book.indicators.VecUtils;
import ru.gustos.trading.visual.Visualizator;

public class OrderBot {
    static Sheet sheet;

    public static void main(String[] args) throws Exception {
        Sheet sheet = TestUtils.makeSheetEmptyLib(0);

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

        int count = 0;
        int profitable = 0;
        int buyIndex = 0;
        for (int i = from;i<to;i++){
            XBar bar = sheet.moments.get(i).bar;

            if (money>0){
                if (bar.getClosePrice()<ema[i]-disp[i]*2)
                    buyOrder = bar.getClosePrice();
                if (bar.getMinPrice()<=buyOrder && bar.getMaxPrice()>=buyOrder){
                    buyOrder = bar.getClosePrice();
                    btc = money/buyOrder*(1-fee);
                    money = 0;
                    buyPrice = buyOrder*(1+fee);
                    count++;
                    buyIndex = i;
                    buyOrder = 0;
                } else
                    buyOrder = ema[i]-disp[i]*2;
            } else if (btc>0){
                if (bar.getClosePrice()>ema[i]+disp[i]*2)
                    sellOrder = bar.getClosePrice();

                if (bar.getMinPrice()<=sellOrder && bar.getMaxPrice()>=sellOrder){
                    sellOrder = bar.getClosePrice();
                    money = btc*sellOrder*(1-fee);
                    btc = 0;
                    System.out.println(String.format("money: %d, time: %d", (int)money,i-buyIndex));
                    boolean good = sellOrder * (1 - fee) / buyPrice > 1;
                    if (good)
                        profitable++;
                    for (int j = buyIndex;j<i;j++)
                        g[j] = good? IIndicator.YES: IIndicator.NO;
                    sellOrder = 0;
                } else
                    sellOrder = ema[i]+disp[i]*2;
            }
        }
        System.out.println(String.format("profitable: %.3g, count: %d", profitable*1.0/count,count));
        sheet.getLib().add("result", IndicatorType.YESNO,g);
        sheet.calcIndicators();

        new Visualizator(sheet);

    }
}
