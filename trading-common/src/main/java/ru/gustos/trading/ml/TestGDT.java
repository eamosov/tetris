package ru.gustos.trading.ml;

import ru.gustos.trading.global.RandomTreeWithExam;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.RandomTree;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class TestGDT{

    static String[] files = new String[]{"diabetes","ionosphere","unbalanced"};

    public static void main(String[] args) throws Exception {

        for (String f : files){
            Instances set = loadArff("testarff/" + f + ".arff");
            testSet(f,set);
        }


    }

    static Instances loadArff(String file) throws IOException {
        BufferedReader reader =
                new BufferedReader(new FileReader(file));
        ArffLoader.ArffReader arff = new ArffLoader.ArffReader(reader);
        Instances data = arff.getData();
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    private static void testSet(String f, Instances set) throws Exception {
        System.out.println(f);
        for (int i = 0;i<3;i++) {
            Instances train = set.trainCV(3, i);
            Instances test = set.testCV(3, i);

            System.out.println("RandomTree");
            Classifier rf = new RandomTree();
//            rf.setMaxDepth(4);
//            rf.setKValue(1000);
            rf.buildClassifier(train);
            Evaluation e = new Evaluation(train);
            e.evaluateModel(rf, test);
            System.out.println("kappa "+e.kappa());
            System.out.println("cm "+ Arrays.deepToString(e.confusionMatrix()));
            System.out.println(rf);

//            int cc = 0;
//            for (int j = 0;j<train.size();j++){
//                if (train.get(j).value(1)>=140)
//                    cc++;
//            }
//            System.out.println(cc+" "+(train.size()-cc));
            System.out.println("GustosTree");
            GustosDecisionTree tree = new GustosDecisionTree();
            tree.buildClassifier(train);
            e = new Evaluation(train);
            e.evaluateModel(tree, test);
            System.out.println("kappa "+e.kappa());
            System.out.println("cm "+ Arrays.deepToString(e.confusionMatrix()));
            System.out.println(tree);
            System.out.println();


        }

    }

}
