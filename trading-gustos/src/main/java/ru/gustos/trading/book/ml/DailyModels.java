package ru.gustos.trading.book.ml;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

import java.time.ZoneId;
import java.time.ZonedDateTime;


public class DailyModels {
    static Sheet sheet;

    public static void main(String[] args) throws Exception {
        sheet = new Sheet();
        sheet.fromCache();
        SheetUtils.FillDecisions(sheet);
        sheet.calcIndicatorsNoPredict();

        calcAll();
    }

    private static void calcAll() throws Exception {
        int first = sheet.getBarIndex(ZonedDateTime.of(2018,2,1,0,0,0,0, ZoneId.systemDefault()));
        int prevday = -1;
        for (int i = first;i<sheet.moments.size();i++){
            ZonedDateTime time = sheet.moments.get(i).bar.getBeginTime();
            int day = time.getDayOfMonth();
            if (day!=prevday){
                calcForDay(i,time);
                prevday = day;
            }
        }
    }

    private static void calcForDay(int ind, ZonedDateTime time) throws Exception {
        int bars = 60*24*7*2*2;
        Instances data = Exporter.makeDataSet(sheet, 1, ind - bars, ind);
        System.out.println(String.format("make for %d %d %d", time.getYear(),time.getMonthValue(),time.getDayOfMonth()));
        Logistic l = new Logistic();
        l.buildClassifier(data);
        weka.core.SerializationHelper.write(String.format("f:/dailymodels/logistics_%d_%d_%d", time.getYear(),time.getMonthValue(),time.getDayOfMonth()) , l);

        NaiveBayes nb = new NaiveBayes();
        nb.buildClassifier(data);
        weka.core.SerializationHelper.write(String.format("f:/dailymodels/nb_%d_%d_%d", time.getYear(),time.getMonthValue(),time.getDayOfMonth()) , nb);

        RandomForest rf = new RandomForest();
        rf.setNumFeatures(1);
        rf.setNumIterations(1500);
        rf.buildClassifier(data);
        weka.core.SerializationHelper.write(String.format("f:/dailymodels/rf_%d_%d_%d", time.getYear(),time.getMonthValue(),time.getDayOfMonth()) , rf);
    }
}
