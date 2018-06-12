package ru.gustos.trading.book.indicators;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.Volumes;

import java.util.ArrayList;
import java.util.List;

public class VolumePikeIndicator extends Indicator{
    public List<double[]> mldata = new ArrayList<>();
//        res.add(new Attribute("upper"));
//        res.add(new Attribute("upper2"));
//        res.add(new Attribute("lower"));
//        res.add(new Attribute("lower2"));
//        res.add(new Attribute("toUpper"));
//        res.add(new Attribute("toUpper2"));
//        res.add(new Attribute("toLower"));
//        res.add(new Attribute("toLower2"));

    public VolumePikeIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
//        Volumes volumes = new Volumes(sheet,false);
//        boolean money = true;
//        double buyPrice = 0;
//        double sellPrice = 0;
//        double stoploss = 0;
//        double gotmoney = 1;
//        int good = 0, bad = 0;
//        double[] ml = new double[9];
//        for (int i = from;i<to;i++){
//            volumes.calc(i);
//            XBar bar = sheet.bar(i);
//            int powClose = volumes.price2pow(bar.getClosePrice());
//            int powOpen = volumes.price2pow(bar.getOpenPrice());
//            int powMin = volumes.price2pow(bar.getMinPrice());
//            int powMax = volumes.price2pow(bar.getMaxPrice());
//            Pair<double[], double[]> vv = volumes.getVolumes();
//            double[] v = VecUtils.ma(VecUtils.add(vv.getFirst(), vv.getSecond(), 1),2);
//            values[0][i] = money?0:1;
////            List<Integer> levels = VecUtils.listLevels(v);
//            if (money) {
//                if (buyPrice>=bar.getMinPrice() && buyPrice<=bar.getMaxPrice()){
//                    money = false;
////                    System.out.println("buy: "+buyPrice+", sell="+sellPrice+", stop="+stoploss);
//                } else {
//                    int level = VecUtils.goToChange(v,powClose,-1);
//                    level = VecUtils.goToChange(v,level,-1);
//                    int upperlevel = VecUtils.goToChange(v,powClose,1);
//                    int upperlevel2 = VecUtils.goToChange(v,upperlevel,1);
//                    if (upperlevel-level>3) {
//                        buyPrice = volumes.pow2price(level);
//                        sellPrice = volumes.pow2price(upperlevel);
//                        sellPrice = buyPrice+(sellPrice-buyPrice)*0.9;
//                        if (sellPrice/buyPrice>1.1) sellPrice = buyPrice*1.1;
//                        level = VecUtils.goToChange(v,level-2,-1);
//                        int lowerlevel = VecUtils.goToChange(v,level-2,-1);
//                        stoploss = volumes.pow2price(level);
//                        if (buyPrice/stoploss>1.1) stoploss = buyPrice/1.1;
//                        ml[0] = v[upperlevel]/v[powClose];
//                        ml[1] = v[upperlevel2]/v[powClose];
//                        ml[2] = v[level]/v[powClose];
//                        ml[3] = v[lowerlevel]/v[powClose];
//                        ml[4] = upperlevel-powClose;
//                        ml[5] = upperlevel2-powClose;
//                        ml[6] = level-powClose;
//                        ml[7] = lowerlevel-powClose;
//
////                        stoploss = buyPrice + (stoploss-buyPrice)*1.1;
//                    } else {
//                        buyPrice = 0;
//                    }
//                }
//            } else {
//                if (stoploss>=bar.getMinPrice()){
//                    money = true;
//                    gotmoney *=stoploss/buyPrice*0.999;
//                    buyPrice = 0;
////                    System.out.println("sell stop: "+gotmoney);
//                    ml[ml.length-1] = 0;
//                    bad++;
//                } else
//                if (sellPrice<=bar.getMaxPrice()){
//                    money = true;
//                    gotmoney *=sellPrice/buyPrice*0.999;
////                    System.out.println("sell: "+gotmoney);
//                    ml[ml.length-1] = 1;
//                    buyPrice = 0;
//                    good++;
//                }
//                if (money)
//                    mldata.add(ml.clone());
//
//            }
//            values[0][i] = money?0:1;
//
//
//        }
//        System.out.println(String.format("got money: %g, good %d, bad %d", gotmoney, good, bad));
    }

}
