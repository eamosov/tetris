package ru.gustos.trading.tests;

import ru.gustos.trading.global.PLHistoryAnalyzer;
import ru.gustos.trading.global.TreePizdunstvo;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.RandomTree;
import weka.core.Instances;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.Arrays;

public class TestPizd{

    public static void main(String[] args) throws Exception {
        try (DataInputStream in = new DataInputStream(new FileInputStream("d:/tetris/pl/pl810.out"))) {
            for (int i = 0;i<4+3;i++)
                new PLHistoryAnalyzer(in);
            TreePizdunstvo.p.load(in);
        } catch (Exception e){
            e.printStackTrace();
        }
        TreePizdunstvo.p.analyze();
        Instances instances = TreePizdunstvo.p.makeSet();
        for (int i = 0;i<5;i++){
            Instances train = instances.trainCV(5, i);
            Instances test = instances.testCV(5, i);
            Classifier rf = new RandomTree();
            ((RandomTree) rf).setKValue(1000);
//            ((RandomTree) rf).setMaxDepth(4);
            rf.buildClassifier(train);
            Evaluation evaluation = new Evaluation(train);
            evaluation.evaluateModel(rf, test);



            System.out.println("kappa: " + evaluation.kappa());
            double[][] cm = evaluation.confusionMatrix();
            System.out.println("win rate: " + cm[1][1]/cm[0][1]);
            System.out.println("confusion: " + Arrays.deepToString(cm));

        }
    }
}
