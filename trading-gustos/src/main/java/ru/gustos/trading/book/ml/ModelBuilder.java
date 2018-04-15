package ru.gustos.trading.book.ml;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.meta.ThresholdSelector;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SelectedTag;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashSet;

public class ModelBuilder {
//    static Sheet sheet;
    public static void main(String[] args) throws Exception {
//        sheet = new Sheet();
//        sheet.fromCache();
//        SheetUtils.FillDecisions(sheet);
//        sheet.calcIndicatorsNoPredict();
        buyModels();
    }

    private static void buyModels() throws Exception {
        BufferedReader br = null;
        br = new BufferedReader(new FileReader("d:/tetrislibs/export_buy_train.arff"));
        Instances trainData = new Instances(br);
        trainData.setClassIndex(trainData.numAttributes() - 1);
        br.close();


        br = new BufferedReader(new FileReader("d:/tetrislibs/export_buy_exam.arff"));

        Instances examData = new Instances(br);
        examData.setClassIndex(examData.numAttributes() - 1);
        br.close();
        String name;
//        for (int i = 1;i<=5;i++) {
//            String name = "buy_rf_"+i+"_1500";
//            System.out.println(name);
//            RandomForest rf = new RandomForest();
//
//            rf.setNumFeatures(i);
//            rf.setNumIterations(1500);
//            trainExamAndPrint(rf, trainData,examData);
//            weka.core.SerializationHelper.write("d:/tetrislibs/models/" + name, rf);
//        }

            name = "buy_rf_1_800";
            System.out.println(name);

            RandomForest rf = new RandomForest();

            rf.setNumFeatures(1);
            rf.setNumIterations(800);
            trainExamAndPrint(rf, trainData,examData);
            weka.core.SerializationHelper.write("d:/tetrislibs/models/" + name, rf);


//        name = "buy_logistic";
//        System.out.println(name);
//        ThresholdSelector ts = new ThresholdSelector();
//        ts.setClassifier(new Logistic());
//        ts.setManualThresholdValue(0.35);
//        trainExamAndPrint(ts, trainData,examData);
//        weka.core.SerializationHelper.write("d:/tetrislibs/models/" + name, ts);
//
//        Classifier c = (Classifier) weka.core.SerializationHelper.read("d:/tetrislibs/models/" + name);
//        trainExamAndPrint(c, trainData,examData,false);

//        name = "buy_bayes";
//        System.out.println(name);
//        NaiveBayes nb = new NaiveBayes();
//        trainExamAndPrint(nb, trainData,examData);
//        weka.core.SerializationHelper.write("d:/tetrislibs/models/" + name, nb);
//
//        name = "buy_nn";
//        System.out.println(name);
//        MultilayerPerceptron nn = new MultilayerPerceptron();
//        nn.setHiddenLayers("6,6,6");
//        nn.setTrainingTime(1500);
//        trainExamAndPrint(nn, trainData,examData);
//        weka.core.SerializationHelper.write("d:/tetrislibs/models/" + name, nn);
    }

    private static void trainExamAndPrint(Classifier classifier, Instances trainData, Instances examData) throws Exception {
        trainExamAndPrint(classifier,trainData,examData,true);
    }

    private static void trainExamAndPrint(Classifier classifier, Instances trainData, Instances examData, boolean train) throws Exception {
        if (train)
            classifier.buildClassifier(trainData);


        Evaluation evaluation = new Evaluation(trainData);
        double vv[] = evaluation.evaluateModel(classifier, examData);

        System.out.println("kappa: " + evaluation.kappa());
        double[][] cm = evaluation.confusionMatrix();
        System.out.println("win rate: " + cm[1][1] / cm[0][1]);
        System.out.println("confusion: " + Arrays.deepToString(cm));
        HashSet<Integer> days = new HashSet<>();
        for (int i =0 ;i<vv.length;i++)
            if (vv[i]>0)
                days.add(i/(60*24));
        System.out.println("days: "+days);

//        int[][] cm1 = new int[2][2];
//        for (int i = 0;i<v.length;i++){
//            int rv = examData.get(i).classValue()>0.5?1:0;
//            int vv = ts.classifyInstance(examData.get(i)) >0.4?1:0;
//            cm1[rv][vv]++;
//        }
//        System.out.println("moved threshold: " + Arrays.deepToString(cm));

    }
}
