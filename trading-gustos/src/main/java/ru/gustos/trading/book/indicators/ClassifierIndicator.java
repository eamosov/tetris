package ru.gustos.trading.book.indicators;

import ru.efreet.trading.logic.impl.sd3.Sd3Logic;
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
    public void calcValues(Sheet sheet, double[] values, int from, int to) {

        Instances data = Exporter.makeDataSet(sheet, 250, from, to);
//        System.out.println(data.size());
        try {

            Classifier c = (Classifier)weka.core.SerializationHelper.read("ml-models/" + classifier);
            Evaluation evaluation = new Evaluation(data);
            double[] result = evaluation.evaluateModel(c, data);

            for (int i = from;i<to;i++)
                values[i] = result[i-from]>0.5?IIndicator.YES:0;

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

