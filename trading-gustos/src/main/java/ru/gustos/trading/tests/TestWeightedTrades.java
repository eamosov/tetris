package ru.gustos.trading.tests;

import ru.gustos.trading.global.PLHistory;
import ru.gustos.trading.global.PLHistoryAnalyzer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.Math.abs;

public class TestWeightedTrades {

    static double all = 1, allw = 1;

    public static void main(String[] args) {
        ArrayList<PLHistory> h = null;
        try (DataInputStream in = new DataInputStream(new FileInputStream("d:/tetrislibs/pl/pl.out"))) {
            h = PLHistoryAnalyzer.loadHistories(in);
        } catch (Exception e){
            e.printStackTrace();
        }

//        double[] res = new double[100];
//        for (int i = 5;i<res.length;i++){
//            PLHistory.test = i;
            all = 1;
            allw = 1;
            calc(h);
//            res[i] = allw;
//        }
        System.out.println("all: "+allw+"/"+all);
//        System.out.println(Arrays.toString(res).replace(',',' '));

    }

    private static void calc(ArrayList<PLHistory> h){
        for (PLHistory hh : h){
            ArrayList<PLHistory.PLTrade> trades = hh.profitHistory;

            double money = 1;
            double moneyw = 1;
            for (int i = 0;i<trades.size();i++){
                PLHistory.PLTrade t = trades.get(i);
                t.tested = hh.shouldBuy(i);
                money*=t.profit;
                if (t.tested)
                    moneyw *= t.profit;

            }

            all*=money;
            allw*=moneyw;
//            System.out.println(hh.instrument+" "+moneyw+"/"+money);
        }
    }

    private static double pos(double n){
        return n>0?n:0;
    }

}

