package ru.gustos.trading.bots;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.IIndicator;
import ru.gustos.trading.book.indicators.TargetBuyIndicator;
import ru.gustos.trading.book.indicators.TargetSellIndicator;

import javax.swing.*;


public class CheatBot {
    Sheet sheet;

    public CheatBot(Sheet sheet){
        this.sheet = sheet;
    }

    public CheatBot run(){
        double money = 1000;
        double btc = 0;

        for (int i = 100;i<sheet.moments.size();i++){
            if (money>0 && sheet.getData().get(TargetBuyIndicator.Id,i)== IIndicator.YES){
                btc += money/sheet.moments.get(i).bar.getOpenPrice()*0.9975;
                money = 0;
            } else if (btc > 0 && (sheet.getData().get(TargetSellIndicator.Id,i)==IIndicator.YES || i==sheet.moments.size()-1)){
                money += btc*sheet.moments.get(i).bar.getOpenPrice()*0.9975;
                btc = 0;
            }
        }
        JOptionPane.showMessageDialog(null,String.format("money x %1$,.2f",(money/1000)));
        return this;
    }
}
