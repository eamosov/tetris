package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class DemaTradeIndicator extends BaseIndicator  {
    public static int Id;

    private int ind;

    public DemaTradeIndicator(IndicatorInitData data){
        super(data);
        Id = data.id;
        ind = data.ind;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return "dematrade";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public Color getColorMax() {
        return Color.green;
    }

    @Override
    public Color getColorMin() {
        return Color.red;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        double money = 1;
        double btc = 0;
        int indicator = ind;
        int sellIndicator = ind;
        double buyPrice = 0;
        double maxprice = 0;
        int buyTick = 0;
        double maxdema = 0;
        boolean op;
        for (int i = 1;i<sheet.moments.size();i++){
            op = false;
            if (money>0){
//                if (sheet.getData().get(indicator,i)>0.1 && sheet.getData().get(indicator,i)<5 && sheet.getData().get(73,i-1)<=0){
                boolean dema = sheet.getData().get(indicator, i) > 0 /*&& sheet.getData().get(indicator, i) < 1.8*/ && sheet.getData().get(indicator, i - 1) <= 0;
                dema |= sheet.getData().get(indicator, i) > sheet.getData().get(indicator, i - 1) && sheet.getData().get(indicator, i - 1)>0;
                XBar bar = sheet.moments.get(i).bar;
                boolean notHigh = true;//sheet.getData().get(indicator, i)>1 || bar.delta()<sheet.moments.get(i-1).bar.deltaMaxMin()*2;
                boolean positive = bar.getOpenPrice()<bar.getClosePrice();
                if (dema && notHigh && positive){
                    op = true;
                    buyPrice = bar.getClosePrice()*1.001;
                    maxprice = buyPrice;
                    btc = money/buyPrice;
                    money = 0;
                    buyTick = i;
                    maxdema = 0;
                }
            } else if (btc>0){
                op = true;
                double price = sheet.moments.get(i).bar.getClosePrice();
                maxprice = Math.max(maxprice, price);
                double dema = sheet.getData().get(sellIndicator, i);
                maxdema = Math.max(dema,maxdema);

                if (dema<0 || (dema<maxdema*0.9) || price<maxprice*0.985){
                    double sellPrice = sheet.moments.get(i).bar.getClosePrice()*0.999;
                    if (sellPrice<buyPrice){
                        for (int j = buyTick;j<=i;j++)
                            values[j] = -1;
                    }

                    money = btc*sellPrice;
                    btc = 0;
                }
            }
            if (values[i]==0)
                values[i] = op?1:0;
        }

    }
}

