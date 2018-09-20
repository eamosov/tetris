package ru.gustos.trading.tests;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import ru.gustos.trading.book.indicators.VecUtils;
import ru.gustos.trading.global.ExperimentData;
import ru.gustos.trading.global.InstrumentData;
import ru.gustos.trading.visual.SimpleCharts;

import java.util.ArrayList;

public class TestBacktesting {

    public static void main(String[] args) {
        ExperimentData experimentData = TestGlobal.init(TestGlobal.instruments,false);
        for (InstrumentData data : experimentData.data) {
            System.out.println(data.instrument);
            SimpleCharts charts = new SimpleCharts(data.instrument.toString(),3);
            for (int r = 0;r<5;r++) {
                int p = (int) (data.size() / 2 + Math.random() * data.size() / 3);
                SimpleRegression regression = new SimpleRegression();
                PolynomialCurveFitter poly = PolynomialCurveFitter.create(2);
                double[] slopeErr = new double[100];
                double[] meanErr = new double[100];
                double[] price = new double[100];
                ArrayList<WeightedObservedPoint> points = new ArrayList<>();
                for (int i = 0; i < slopeErr.length; i++) {
                    double pp = data.bar(p - i).getClosePrice();
                    price[i] = pp;

                    points.add(new WeightedObservedPoint(1,i,pp));



                    regression.addData(i, pp);
                    if (i>4) {
                        meanErr[i] = regression.getMeanSquareError();

                        double[] fit = poly.fit(points);
                        double err  = 0;
                        for (int j = 0;j<points.size();j++){
                            double v = fit[0]+fit[1]*j+fit[2]*j*j;
                            err+=(v-points.get(j).getY())*(v-points.get(j).getY());
                        }
                        err/=points.size();
                        slopeErr[i] = err;//regression.getSignificance();

                    }
                }
                price = VecUtils.norm(price);

                charts.addChart("price "+r,price);
                charts.addChart("poly "+r,slopeErr);
                charts.addChart("linear "+r,meanErr);
            }

        }
    }

}
