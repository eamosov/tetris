package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.ml.Exporter;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

import static ru.gustos.trading.book.ml.Exporter.doExport;

public class ClassifierIndicator extends Indicator {
    public ClassifierIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {

        Instances data = Exporter.makeDataSet(sheet, 250, from, to);
//        System.out.println(data.size());
        try {

            Classifier c = (Classifier)weka.core.SerializationHelper.read("ml-models/" + this.data.classifier);
            Evaluation evaluation = new Evaluation(data);
            double[] result = evaluation.evaluateModel(c, data);

            for (int i = from;i<to;i++)
                values[0][i] = result[i-from]>0.5? Indicator.YES:0;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

