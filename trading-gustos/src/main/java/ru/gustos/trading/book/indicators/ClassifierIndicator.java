package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.ml.Exporter;
import ru.gustos.trading.visual.CandlesPane;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;

import static ru.gustos.trading.book.ml.Exporter.doExport;
import static ru.gustos.trading.book.ml.Exporter.string2file;

public class ClassifierIndicator extends BaseIndicator{
    String classifier;
    public ClassifierIndicator(IndicatorInitData data){
        super(data);
        classifier = data.classifier;
    }

    @Override
    public String getName() {
        return classifier;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        System.out.println("working on classifier "+getName());
        String s = doExport(sheet, TargetBuyIndicator.Id,false);
        string2file("d:/tetrislibs/tempexam.arff",s);
        BufferedReader br = null;
        Arrays.fill(values,0);
        try {
            br = new BufferedReader(new FileReader("d:/tetrislibs/tempexam.arff"));
            Instances examData = new Instances(br);
            examData.setClassIndex(examData.numAttributes() - 1);
            br.close();

            Classifier c = (Classifier)weka.core.SerializationHelper.read("d:/tetrislibs/models/" + classifier);
            Evaluation evaluation = new Evaluation(examData);
            double[] result = evaluation.evaluateModel(c, examData);

            for (int i = 0;i<result.length;i++)
                values[Exporter.lastFrom+i] = result[i]>0.5?IIndicator.YES:0;

        } catch (Exception e) {
            e.printStackTrace();
        }

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
