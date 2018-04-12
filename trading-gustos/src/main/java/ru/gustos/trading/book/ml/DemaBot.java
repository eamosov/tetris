package ru.gustos.trading.book.ml;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.indicators.IIndicator;
import weka.core.Instances;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class DemaBot {
    static Sheet sheet;

    public static void main(String[] args) throws Exception {
        Sheet sheet = new Sheet();
        sheet.fromCache(500);
//            sheet.fromExchange();
        SheetUtils.FillDecisions(sheet);
        sheet.calcIndicators();

//        int from = 12000;
        int from = 0;
        int to = sheet.moments.size();
        double money = 1000;
        double btc = 0;
        int indicator = 73;
        int sellIndicator = 70;
        double buyPrice = 0;
        double maxprice = 0;

        for (int i = from;i<to;i++){
            if (money>0){
//                if (sheet.getData().get(indicator,i)>0.1 && sheet.getData().get(indicator,i)<5 && sheet.getData().get(73,i-1)<=0){
                if (sheet.getData().get(62,i)>0 && sheet.getData().get(indicator,i)>0.05 && sheet.getData().get(indicator,i)<0.8 && sheet.getData().get(indicator,i-1)<=0){
                    System.out.println("buy "+sheet.getData().get(indicator,i));
                    buyPrice = sheet.moments.get(i).bar.getClosePrice()*1.0005;
                    maxprice = buyPrice;
                    btc = money/buyPrice;
                    money = 0;
                }
            } else if (btc>0){
                double price = sheet.moments.get(i).bar.getClosePrice();
                maxprice = Math.max(maxprice, price);

                if (sheet.getData().get(sellIndicator,i)<0 || (sheet.getData().get(sellIndicator,i)<sheet.getData().get(sellIndicator,i-1)*0.2) || price<maxprice*0.985){
                    double sellPrice = sheet.moments.get(i).bar.getClosePrice()*0.9995;
                    money = btc*sellPrice;
                    btc = 0;
                    System.out.println(String.format("operation: money %.4g, profit %.4g, time %s", money,buyPrice/sellPrice,sheet.moments.get(i).bar.getBeginTime().toString()));
                }
            }
        }


    }
}
