package ru.gustos.trading.book.ml;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;

public class DemaBot {
    static Sheet sheet;

    public static void main(String[] args) throws Exception {
        Sheet sheet = TestUtils.makeSheet();

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

        int count = 0;
        int profitable = 0;
        for (int i = from;i<to;i++){
            if (money>0){
//                if (sheet.getData().get(indicator,i)>0.1 && sheet.getData().get(indicator,i)<5 && sheet.getData().get(73,i-1)<=0){
                boolean dema = sheet.getData().get(indicator, i) > 0.05 && sheet.getData().get(indicator,i)<0.8 && sheet.getData().get(indicator, i - 1) <= 0;
                XBar bar = sheet.moments.get(i).bar;
                boolean notHigh = true;//(bar.getClosePrice()-bar.getOpenPrice())<sheet.moments.get(i-1).bar.deltaMaxMin()*4;
                boolean positive = bar.getOpenPrice()<bar.getClosePrice();
                if (dema && notHigh && positive){
                    System.out.println("buy "+sheet.getData().get(indicator,i));
                    buyPrice = sheet.moments.get(i).bar.getClosePrice()*1.001;
                    maxprice = buyPrice;
                    btc = money/buyPrice;
                    money = 0;
                    maxdema = 0;
                    count++;
                }
            } else if (btc>0){
                double price = sheet.moments.get(i).bar.getClosePrice();
                maxprice = Math.max(maxprice, price);
                double dema = sheet.getData().get(sellIndicator, i);
                maxdema = Math.max(dema,maxdema);
                if (dema <0 || (dema <maxdema*0.75) || price<maxprice*0.98){
                    double sellPrice = sheet.moments.get(i).bar.getClosePrice()*0.999;
                    money = btc*sellPrice;
                    btc = 0;
                    if (sellPrice>buyPrice)
                        profitable++;
                    System.out.println(String.format("operation: money %.4g, profit %.4g, time %s", money,buyPrice/sellPrice,sheet.moments.get(i).bar.getBeginTime().toString()));
                }
            }
        }
        System.out.println(String.format("profitable: %.3g", profitable*1.0/count));


    }
}

