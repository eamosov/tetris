package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.ml.Exporter;
import ru.gustos.trading.visual.CandlesPane;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import static ru.gustos.trading.book.SheetUtils.sellValues;
import static ru.gustos.trading.book.ml.Exporter.doExport;
import static ru.gustos.trading.book.ml.Exporter.string2file;

public class PredictBuyIndicator extends BaseIndicator{
    public static int Id;

    public PredictBuyIndicator(IndicatorInitData data){
        super(data);
        Id = data.id;
    }

    @Override
    public String getName() {
        return "YBuy";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
//        String s = doExport(sheet, TargetBuyIndicator.Id,true);
//        string2file("d:/tetrislibs/temptrain.arff",s);
//        s = doExport(sheet, TargetBuyIndicator.Id,false);
//        string2file("d:/tetrislibs/tempexam.arff",s);
//        BufferedReader br = null;
//        Arrays.fill(values,0);
//        try {
//            br = new BufferedReader(new FileReader("d:/tetrislibs/temptrain.arff"));
//            Instances trainData = new Instances(br);
//            trainData.setClassIndex(trainData.numAttributes() - 1);
//            br.close();
//            br = new BufferedReader(new FileReader("d:/tetrislibs/tempexam.arff"));
//            Instances examData = new Instances(br);
//            examData.setClassIndex(examData.numAttributes() - 1);
//            br.close();
//
//            System.out.println("random forest for ybuy");
//            RandomForest rf = new RandomForest();
//            rf.setNumFeatures(2);
//            rf.setNumIterations(500);
//            rf.buildClassifier(trainData);
//            Evaluation evaluation = new Evaluation(trainData);
//            double[] result = evaluation.evaluateModel(rf, examData);
//
//            System.out.println("kappa: " + evaluation.kappa());
//            double[][] cm = evaluation.confusionMatrix();
//            System.out.println("win rate: " + cm[1][1]/cm[0][1]);
//            System.out.println("confusion: " + Arrays.deepToString(cm));
//            for (int i = 0;i<result.length;i++)
//                values[Exporter.lastFrom+i] = result[i]>0.5?IIndicator.YES:0;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }

    @Override
    public Color getColorMin() {
        return Color.darkGray;
    }
}

