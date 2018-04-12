package ru.gustos.trading.book.indicators;

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
        int sellIndicator = 70;
        double buyPrice = 0;
        double maxprice = 0;
        int buyTick = 0;
        for (int i = 1;i<sheet.moments.size();i++){
            if (money>0){
//                if (sheet.getData().get(indicator,i)>0.1 && sheet.getData().get(indicator,i)<5 && sheet.getData().get(73,i-1)<=0){
                if (sheet.getData().get(indicator,i)>0.05 && /*sheet.getData().get(indicator,i)<0.8 && */sheet.getData().get(indicator,i-1)<=0){
                    buyPrice = sheet.moments.get(i).bar.getClosePrice()*1.001;
                    maxprice = buyPrice;
                    btc = money/buyPrice;
                    money = 0;
                    buyTick = i;
                }
            } else if (btc>0){
                double price = sheet.moments.get(i).bar.getClosePrice();
                maxprice = Math.max(maxprice, price);

                if (sheet.getData().get(sellIndicator,i)<0 || (sheet.getData().get(sellIndicator,i)<sheet.getData().get(sellIndicator,i-1)*0.2) || price<maxprice*0.985){
                    double sellPrice = sheet.moments.get(i).bar.getClosePrice()*0.999;
                    if (sellPrice<buyPrice){
                        for (int j = buyTick;j<i;j++)
                            values[j] = -1;
                    }

                    money = btc*sellPrice;
                    btc = 0;
                }
            }
            values[i] = money>0?0:1;
        }

    }
}

