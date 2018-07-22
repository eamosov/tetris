package ru.gustos.trading.book;

import kotlin.Pair;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.stream.Collectors;

public class Extrapolation{
    BarsSource bars;
    int pos;

    int baseLength;

    public double err0;
    public double err1;
    public double errbase;

    Model model;


    public Extrapolation(BarsSource bars, int pos, int length){
        this.baseLength = length;
        this.bars = bars;
        this.pos = pos;
        expandBack();
        expandForward();
    }

    public int from(){
        return model.from;
    }

    public int to(){
        return model.to;
    }

    public double value(double x){
        return model.value(x);
    }

    private void expandBack(){

        model = new Model(pos - baseLength, pos - 2);
        if (model.meanErr>400) return;
        Model nm = model,pnm;
        int back = baseLength;
        do {
            back++;
            pnm = nm;
            nm = new Model(pos-back,pos-2);
        } while (nm.meanErr<model.meanErr*1.25);

        model = pnm;

        errbase = model.meanErr;
        err1 = new Model(pos-back,pos-1).meanErr;
        err0 = new Model(pos-back,pos).meanErr;
    }

    private void expandForward(){
        if (model.meanErr>400) return;
        int was = model.to;
        while (model.err(model.to+1)<model.meanErr*1.1) model.to++;
        System.out.println("future predicted: "+(model.to-was));
    }

    private ArrayList<WeightedObservedPoint> makePoints(int from, int to, HashSet<Integer> filter){
        ArrayList<WeightedObservedPoint> points = new ArrayList<>();
        for (int i = from;i<to;i++) if (!filter.contains(i))
            points.add(new WeightedObservedPoint(1,i-from,bars.bar(i).getClosePrice()));
        return points;

    }

    private double meanSq(ArrayList<WeightedObservedPoint> points, double[] kk){
        double err = 0;
        for (int i = 0;i<points.size();i++){
            WeightedObservedPoint p = points.get(i);
            double v = calc(kk,p.getX());//kk[0]+kk[1]*p.getX() + kk[2]*p.getX()*p.getX();
            err+=(v-p.getY())*(v-p.getY());
        }
        return err/points.size();
    }

    private double calc(double[] kk, double x){
        double xx = 1;
        double res = 0;
        for (int i = 0;i<kk.length;i++){
            res+=kk[i]*xx;
            xx*=x;
        }
        return res;
    }


    class Model {
        double[] kk;
        int from, to;
        double meanErr;
        HashSet<Integer> filter = new HashSet<>();

        Model(int from, int to){
            this.from = from;
            this.to = to;
            PolynomialCurveFitter poly = PolynomialCurveFitter.create(2);

            ArrayList<WeightedObservedPoint> pp = makePoints(from, to, filter);
            for (int i = 0;i<3;i++) {
                kk = poly.fit(pp);
                filterWorst(pp, 3);
                meanErr = meanSq(pp, kk);
            }
        }

        private void filterWorst(ArrayList<WeightedObservedPoint> pp, int cnt) {
            ArrayList<Pair<Integer,Double>> temp = new ArrayList<>();
            for (int i = 5;i<pp.size()-5;i++){
                temp.add(new Pair<>(i,err(pp.get(i))));
            }
            temp.sort(Comparator.comparing(Pair::getSecond));
            temp.subList(0,temp.size()-cnt).clear();
            temp.sort(Comparator.comparing(Pair::getFirst));
            for (int i = 0;i<temp.size();i++)
                pp.remove((int)temp.get(temp.size()-i-1).getFirst());
        }

        private double err(int index) {
            return err(new WeightedObservedPoint(1,index-from,bars.bar(index).getClosePrice()));
        }

        private double err(WeightedObservedPoint p) {
            double v = value(p.getX()+from)-p.getY();
            return v*v;
        }


        public double value(double x) {
            x = x-from;
            return calc(kk,x);
        }
    }
}
