package ru.gustos.trading.global;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class CalcConfig{
    public int goodMoments = 6;
    public int badMoments = 12;
    public int treesBuy = 1000;
    public int treesSell = 1000;
    public int kValueBuy = 0;
    public int kValueSell = 0;
    public double momentLimit = 0;
    public int learnIntervalBuy = 180;
    public int learnIntervalSell = 180;
    public int maxDepth = 10;


    public static CalcConfig load(String path) throws IOException {
        return new Gson().fromJson(FileUtils.readFileToString(new File(path)),CalcConfig.class);

    }

    @Override
    public String toString() {
        return String.format("goodMoments: %d, badMoments: %d, treesBuy: %d, treesSell: %d, kValueBuy: %d, kValueSell: %d, limit: %.3g, learnIntervalBuy: %d, learnIntervalSell: %d, maxDepth: %d", goodMoments,badMoments, treesBuy, treesSell, kValueBuy, kValueSell,momentLimit, learnIntervalBuy, learnIntervalSell, maxDepth);
    }

}
