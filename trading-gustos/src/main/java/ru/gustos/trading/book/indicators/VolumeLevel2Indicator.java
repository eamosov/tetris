package ru.gustos.trading.book.indicators;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.Volumes;

public class VolumeLevel2Indicator extends Indicator{

    public VolumeLevel2Indicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        Volumes volumes = new Volumes(sheet,false);
        boolean money = true;
        int buypow = 0;
        int targetmax = 0;
        int stoploss = 0;
        int stoploss2 = 0;
        boolean visitMiddle = false;
        boolean wentdown = false;
        for (int i = from;i<to;i++){
            volumes.calc(i);
            XBar bar = sheet.bar(i);
            int powClose = volumes.price2pow(bar.getClosePrice());
            int powOpen = volumes.price2pow(bar.getOpenPrice());
            int powMin = volumes.price2pow(bar.getMinPrice());
            int powMax = volumes.price2pow(bar.getMaxPrice());
            Pair<double[], double[]> vv = volumes.getGustosVolumes();
            double[] v = VecUtils.ma(VecUtils.add(vv.getFirst(), vv.getSecond(), 1),8);
            values[0][i] = money?0:1;
            if (money) {

                int min = VecUtils.goDownToMinimum(v,powClose);
                if (min< powMin || min> powMax) continue;
                int max = VecUtils.findMaximum(v,min,1);
                int lowerMax = VecUtils.findMaximum(v,min,-1);


                if (max==-1 || lowerMax==-1) continue;
                if (v[max]<v[min]*1.3) continue;
                if (v[lowerMax]<v[min]*1.2) continue;
//                if (powOpen<min) continue;
                if (powClose>min) continue;
                if (vv.getSecond()[lowerMax]<vv.getSecond()[max]*0.05) continue;
                if (sheet.whenPriceWas(i,volumes.pow2price(max))<=sheet.whenPriceWas(i,volumes.pow2price(lowerMax))) continue;
                if (sheet.whenPriceWas(i,volumes.pow2price(max))<=sheet.whenPriceWas(i-1,volumes.pow2price(min))) continue;
                money = false;
                visitMiddle = false;
                buypow = min;
                targetmax = min+(max-min)*4/5;
                stoploss = min-(min-lowerMax)*2;
                stoploss2 = lowerMax;
            } else {
                int middle = (buypow+targetmax)/2;
                if (targetmax>=powMin && targetmax<=powMax)
                    money = true;
                else if (stoploss<=powMax)
                    money = true;
                else if (visitMiddle && buypow<=powMax)
                    stoploss = stoploss2;
                if (!money) {
                    if (middle >= powMin && middle <= powMax)
                        visitMiddle = true;
                }



            }
            values[0][i] = money?0:1;


        }
    }

}

