package ru.gustos.trading.book.ml;

import weka.classifiers.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogisticImprover{
    Instances set;
    int maxPow;

    int[] pows;
    double bestKappa = -1;

    public LogisticImprover(Instances set, int maxPow){
        this.set = set;
        this.maxPow = maxPow;
        pows = new int[set.numAttributes()-1];
        Arrays.fill(pows,1);
    }

    public void doIt(){
        boolean foundBetter = false;
        double best = -1;
        int[] bestPows = pows.clone();
        do {
            foundBetter = false;
            for (int i = 0;i<pows.length;i++){
                if (pows[i]>0){
                    pows[i]--;
                    double k = calc();
                    if (k>best){
                        best = k;
                        bestPows = pows.clone();
                        foundBetter = true;
                    } else {
                        pows[i]++;
                    }
                }
                if (pows[i]<maxPow){
                    pows[i]++;
                    double k = calc();
                    if (k>best){
                        best = k;
                        bestPows = pows.clone();
                        foundBetter = true;
                    } else {
                        pows[i]--;
                    }

                }
            }
        } while (foundBetter);
        pows = bestPows;
        bestKappa = best;
    }

    private int sum(){
        return Arrays.stream(pows).sum();
    }

    private ArrayList<Attribute> makeAttributes(){
        ArrayList<Attribute> result = new ArrayList<>();
        for (int i = 0; i< pows.length; i++){
            for (int j = 0; j< pows[i]; j++){
                result.add(new Attribute(set.attribute(i).name()+"_"+(j+1)));
            }
        }
        result.add(set.attribute(set.numAttributes()-1));
        return result;

    }

    public Instances prepare(){
        Instances result = new Instances("data", makeAttributes(), set.size());
        for (Instance ii : set) {
            int n = 0;
            double[] d = new double[result.numAttributes()];
            for (int i = 0; i < pows.length; i++) {
                for (int j = 0; j < pows[i]; j++) {
                    double v = ii.value(i);
                    d[n] = v;
                    for (int k = 1;k<j;k++)
                        d[n]*=v;

                    n++;
                }
            }
            d[d.length-1] = ii.value(ii.numAttributes()-1);
            result.add(new DenseInstance(1,d));
        }
        result.setClassIndex(result.numAttributes()-1);
        return result;
    }

    private double calc(){
        Instances data = prepare();
        double min = 1;
        double sum = 0;
        try {

            for (int i = 0;i<10;i++){
                Instances train = data.trainCV(10, i);
                Instances test = data.testCV(10, i);
                Logistic l = new Logistic();
                l.buildClassifier(train);
                Evaluation evaluation = new Evaluation(test);
                evaluation.evaluateModel(l, test);
                double kappa = evaluation.kappa();
                if (kappa<min)
                    min = kappa;
                sum+=kappa;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("calc "+Arrays.toString(pows)+" result: "+sum/10+", "+min);
        return (sum/10+min)/2;
    }


}
