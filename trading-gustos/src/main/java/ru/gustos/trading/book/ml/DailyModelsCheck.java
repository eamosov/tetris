package ru.gustos.trading.book.ml;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.indicators.TargetBuyIndicator;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;


public class DailyModelsCheck {
    static Sheet sheet;

    static Classifier logistic;
    static Classifier bayes;
    static Classifier rf;

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
        Instances instances = Exporter.makeDataSet(sheet, TargetBuyIndicator.Id, first, sheet.moments.size());
        int[][] cm1 = new int[2][2];
        int[][] cm2 = new int[2][2];
        int[][] cm3 = new int[2][2];
        int[][] cm1_ = new int[2][2];
        int[][] cm2_ = new int[2][2];
        int[][] cm3_ = new int[2][2];
        for (int i = first;i<sheet.moments.size();i++){
            ZonedDateTime time = sheet.moments.get(i).bar.getBeginTime();
            int day = time.getDayOfMonth();
            if (day!=prevday){
                prevday = day;
                rf = null;
                bayes = null;
                logistic = null;
                logistic = (Classifier) weka.core.SerializationHelper.read(String.format("f:/dailymodels/logistics_%d_%d_%d", time.getYear(),time.getMonthValue(),time.getDayOfMonth()));
                bayes = (Classifier) weka.core.SerializationHelper.read(String.format("f:/dailymodels/nb_%d_%d_%d", time.getYear(),time.getMonthValue(),time.getDayOfMonth()));
                rf = (Classifier) weka.core.SerializationHelper.read(String.format("f:/dailymodels/rf_%d_%d_%d", time.getYear(),time.getMonthValue(),time.getDayOfMonth()));
                System.out.println("logistics: "+ Arrays.deepToString(cm1_));
                System.out.println("bayes: "+ Arrays.deepToString(cm2_));
                System.out.println("rf: "+ Arrays.deepToString(cm3_));
                cm1_ = new int[2][2];
                cm2_ = new int[2][2];
                cm3_ = new int[2][2];
            }
            int rv = sheet.getData().get(TargetBuyIndicator.Id, i)>0.5?1:0;
            Instance inst = instances.instance(i - first);
            classify(inst, rv,logistic,cm1,cm1_);
            classify(inst,rv, bayes,cm2,cm2_);
            classify(inst,rv, rf,cm3,cm3_);
        }
        System.out.println("final logistics: "+ Arrays.deepToString(cm1));
        System.out.println("final bayes: "+ Arrays.deepToString(cm2));
        System.out.println("final rf: "+ Arrays.deepToString(cm3));
    }

    private static void classify(Instance instance, int rv, Classifier c, int[][] cm, int[][] cm_) throws Exception {
        int v = c.classifyInstance(instance)>0.5?1:0;
        cm[rv][v]++;
        cm_[rv][v]++;
    }
}
