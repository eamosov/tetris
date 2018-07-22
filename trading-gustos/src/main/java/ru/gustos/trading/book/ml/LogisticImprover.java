package ru.gustos.trading.book.ml;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;

public class LogisticImprover{
    Instances set;
    Instances testSet;
    int maxPow;

    int[] pows;
    double bestKappa = -1;

    public LogisticImprover(Instances set, Instances testSet, int maxPow){
        this.set = set;
        this.testSet = testSet;
        this.maxPow = maxPow;
        pows = new int[set.numAttributes()-1];
        Arrays.fill(pows,1);

    }
    public LogisticImprover(Instances set, int maxPow){
        this(set,null,maxPow);
    }

    class WhatToAddResult {
        int[] pows;
        double kappa, examKappa;
        String result;
        String examResult;
        int count;
        Classifier classifier;

        public String toString(){
            return String.format("attribute: %s, kappa %g(%g), win: %s", pows2string(pows),examKappa,kappa, examResult);
        }

        public void exam() throws Exception {
            LogisticImprover.this.pows = pows;
            Instances test = prepare(testSet);
            int[][] confusion = evaluate(classifier,test);
//                double kappa = evaluation.kappa();
//                double[][] confusion = evaluation.confusionMatrix();
            examResult = confusion[1][1]+"/"+confusion[0][1];
//                sum = min = confusion[1][1]/confusion[0][1]*10;///kappa;
            examKappa = kappa(confusion);

        }
    }

    public String pows2string(int[] pows){
        StringBuilder sb = new StringBuilder();
        for (int i = 0;i<pows.length;i++) if (pows[i]!=0){
            if (sb.length()!=0) sb.append(" + ");
            sb.append(set.attribute(i).name()).append("^").append(pows[i]);
        }
        return sb.toString();
    }
    private ArrayList<WhatToAddResult> whatToAdd(int[] p, int top){
        ArrayList<WhatToAddResult> result = new ArrayList<>();
        pows = p.clone();


        for (int i = 0;i<pows.length;i++) if (pows[i]==0){
            WhatToAddResult best = new WhatToAddResult();
            best.kappa = -2;

            for (int j = 1;j<=maxPow;j++) {
                pows[i] = j;
                double k = calc();
                if (best.kappa<k){
                    best.kappa = k;
                    best.classifier = classifier;
                    best.pows = pows.clone();
                    best.result = calcResult;
                }
            }
            best.count = count();
            result.add(best);
            pows[i] = 0;
        }
        result.sort((c1,c2)->Double.compare(c2.kappa,c1.kappa));
        while (result.size()>top) result.remove(result.size()-1);
        return result;
    }

    public ArrayList<WhatToAddResult> doIt(boolean print){

        ArrayList<WhatToAddResult> results = whatToAdd(new int[pows.length], 200);
        ArrayList<WhatToAddResult> temp = new ArrayList<>();
        for (int i = 0;i<5;i++)
            temp.addAll(whatToAdd(results.get(i).pows, 3));

        int count = 2;
        do {
            results.addAll(temp);
            temp.clear();
            sortAndFilter(results);
            for (int i = 0; i < 5; i++)
                if (results.get(i).count == count)
                    temp.addAll(whatToAdd(results.get(i).pows, 3));
            count++;
        } while (temp.size()>0 && count<3);

        try {
            for (WhatToAddResult w : results) {
                w.exam();
                if (print)
                    System.out.println(w.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private void sortAndFilter(ArrayList<WhatToAddResult> results) {
        results.sort((c1,c2)->Double.compare(c2.kappa,c1.kappa));
        for (int i =results.size()-2;i>=0;i--)
            if (Arrays.equals(results.get(i).pows,results.get(i+1).pows)) results.remove(i+1);
    }

    private int sum(){
        return Arrays.stream(pows).sum();
    }

    private int count(){
        return Arrays.stream(pows).map(i->i>0?1:0).sum();
    }

    private int size(){
        int nn = 0;
        for (int i = 0; i< pows.length; i++)
            nn+=pows[i];
        return nn+1;
    }

    public ArrayList<Attribute> makeAttributes(){
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
        return prepare(set);
    }
    public Instances prepare(Instances set){
        Instances result = new Instances("data", makeAttributes(), set.size());
        for (Instance ii : set)
            result.add(prepareInstance(ii.toDoubleArray()));

        result.setClassIndex(result.numAttributes()-1);
        return result;
    }

    public DenseInstance prepareInstance(double[] ii) {
        int n = 0;
        double[] d = new double[size()];
        for (int i = 0; i < pows.length; i++) {
            for (int j = 0; j < pows[i]; j++) {
                double v = ii[i];
                d[n] = v;
                for (int k = 0;k<j;k++)
                    d[n]*=v;

                n++;
            }
        }
        d[d.length-1] = ii[ii.length-1];
        return new DenseInstance(1,d);
    }


    private String calcResult = null;
    private Classifier classifier = null;
    private double calc(){
        double min = 1;
        double sum = 0;
        try {
            if (testSet==null) {
                Instances data = prepare();
                for (int i = 0; i < 5; i++) {
                    Instances train = data.trainCV(5, i);
                    Instances test = data.testCV(5, i);
                    Logistic l = new Logistic();
                    l.buildClassifier(train);
                    Evaluation evaluation = new Evaluation(test);
                    evaluation.evaluateModel(l, test);
                    double kappa = evaluation.kappa();
                    if (kappa < min)
                        min = kappa;
                    sum += kappa;
                }
                sum = sum/5;
            } else {
                classifier = new Logistic();
                Instances prepared = prepare();
                classifier.buildClassifier(prepared);
                int[][] confusion = evaluate(classifier, prepared);
                calcResult = confusion[1][1]+"/"+confusion[0][1];
                sum = min = kappa(confusion);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        System.out.println("calc "+Arrays.toString(pows)+" result: "+sum/10+", "+min);
        return sum;
//        return (sum/10+min)/2;
    }

    private int[][] evaluate(Classifier l, Instances test) throws Exception {
        int[][] res = new int[2][2];
        for (int i = 0;i<test.numInstances();i++) {
            Instance inst = test.instance(i);
            boolean r = l.distributionForInstance(inst)[1]>0.5;
            res[inst.value(test.numAttributes()-1)==0?0:1][r?1:0]++;
        }
        return res;
    }

    private double kappa(int[][] matrix){
        double[] sumRows = new double[matrix.length];
        double[] sumColumns = new double[matrix.length];
        double sumOfWeights = 0.0D;

        for(int i = 0; i < matrix.length; ++i) {
            for(int j = 0; j < matrix.length; ++j) {
                sumRows[i] += matrix[i][j];
                sumColumns[j] += matrix[i][j];
                sumOfWeights += matrix[i][j];
            }
        }

        double correct = 0.0D;
        double chanceAgreement = 0.0D;

        for(int i = 0; i < matrix.length; ++i) {
            chanceAgreement += sumRows[i] * sumColumns[i];
            correct += matrix[i][i];
        }

        chanceAgreement /= sumOfWeights * sumOfWeights;
        correct /= sumOfWeights;
        if (chanceAgreement < 1.0D) {
            return (correct - chanceAgreement) / (1.0D - chanceAgreement);
        } else {
            return 1.0D;
        }

    }


}
