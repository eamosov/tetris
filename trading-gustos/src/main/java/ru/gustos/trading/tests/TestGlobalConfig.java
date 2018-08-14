package ru.gustos.trading.tests;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class TestGlobalConfig {
    public int goodMoments = 14;
    public int badMoments = 14;
    public int trees = 50;
    public double threshold = 0.5;

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
        return String.format("goodMoments: %d, badMoments: %d, trees: %d, threshold: %.3g", goodMoments,badMoments, trees, threshold);
    }
}
