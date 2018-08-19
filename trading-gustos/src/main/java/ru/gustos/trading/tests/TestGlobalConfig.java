package ru.gustos.trading.tests;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class TestGlobalConfig {
    public int goodMoments = 14;
    public int badMoments = 14;
    public int treesBuy = 250;
    public int treesSell = 250;
    public double threshold = 0.5;
    public int kValueBuy = 0;
    public int kValueSell = 0;
    public int momentsInterval = 40;
    public double momentLimit = 0.03;
    public int learnIntervalBuy = 180;
    public int learnIntervalSell = 180;
    public int maxDepth = 0;

    static TestGlobalConfig config;

    static {
        try {
            config = new Gson().fromJson(FileUtils.readFileToString(new File("testconf.json")),TestGlobalConfig.class);
            System.out.println("loaded config "+config);
        } catch (IOException e) {
            System.out.println("no testconf.json, using defaults");
            config = new TestGlobalConfig();

        }
    }

    @Override
    public String toString() {
        return String.format("goodMoments: %d, badMoments: %d, treesBuy: %d, treesSell: %d, threshold: %.3g, kValueBuy: %d, kValueSell: %d, momentsInterval: %d, limit: %.3g, learnIntervalBuy: %d, learnIntervalSell: %d, maxDepth: %d", goodMoments,badMoments, treesBuy, treesSell, threshold, kValueBuy, kValueSell,momentsInterval, momentLimit, learnIntervalBuy, learnIntervalSell, maxDepth);
    }
}
