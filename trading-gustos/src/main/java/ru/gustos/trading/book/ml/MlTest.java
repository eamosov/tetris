package ru.gustos.trading.book.ml;

import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.functions.Logistic;

import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class MlTest {

    static void do1() throws Exception{
        BufferedReader br = null;
        br = new BufferedReader(new FileReader("d:/tetrislibs/export_buy_train.arff"));
        Instances trainData = new Instances(br);
        trainData.setClassIndex(trainData.numAttributes() - 1);
        br.close();


        br = new BufferedReader(new FileReader("d:/tetrislibs/export_buy_exam.arff"));

        Instances examData = new Instances(br);
        examData.setClassIndex(examData.numAttributes() - 1);
        br.close();
        for (int j = 1;j<=4;j++)
            for (int i = 1;i<=5;i++) {
                RandomForest rf = new RandomForest();
                rf.setNumFeatures(i);
                rf.setNumIterations(500*j);
                rf.buildClassifier(trainData);
                rf.setComputeAttributeImportance(true);
                CostMatrix m = new CostMatrix(2);

                CostSensitiveClassifier cc = new CostSensitiveClassifier();
                Evaluation evaluation = new Evaluation(trainData);
                evaluation.evaluateModel(rf, examData);



                System.out.println("features: "+i);
                System.out.println("iterations: "+(500*j));
                System.out.println("kappa: " + evaluation.kappa());
                double[][] cm = evaluation.confusionMatrix();
                System.out.println("win rate: " + cm[1][1]/cm[0][1]);
                System.out.println("confusion: " + Arrays.deepToString(cm));
            }

    }

    static void do2() throws Exception{
        BufferedReader br = null;
        br = new BufferedReader(new FileReader("d:/tetrislibs/export_sell_train.arff"));
        Instances trainData = new Instances(br);
        trainData.setClassIndex(trainData.numAttributes() - 1);
        br.close();


        br = new BufferedReader(new FileReader("d:/tetrislibs/export_sell_exam.arff"));

        Instances examData = new Instances(br);
        examData.setClassIndex(examData.numAttributes() - 1);
        br.close();
        for (int j = 1;j<=6;j++)
            for (int i = 1;i<=5;i++) {
                RandomForest rf = new RandomForest();
                rf.setNumFeatures(i);
                rf.setNumIterations(500*j);
                rf.buildClassifier(trainData);


                Evaluation evaluation = new Evaluation(trainData);
                evaluation.evaluateModel(rf, examData);

                System.out.println("features: "+i);
                System.out.println("iterations: "+(500*j));
                System.out.println("kappa: " + evaluation.kappa());
                double[][] cm = evaluation.confusionMatrix();
                System.out.println("win rate: " + cm[1][1]/cm[0][1]);
                System.out.println("confusion: " + Arrays.deepToString(cm));
            }

    }

    public static void main(String[] args) throws Exception {
        do1();
        do2();
    }
}


