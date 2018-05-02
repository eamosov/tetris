package ru.gustos.trading.book.indicators;

import kotlin.Pair;

import java.io.*;
import java.util.ArrayList;

public class VecUtils {


    public static double avg(double[] v){
        double sum = 0;
        for (int i = 0;i<v.length;i++)
            sum+=v[i];
        return sum/v.length;
    }

    public static double avg(double[] v, int from, int count) {
        double sum = 0;
        count = Math.min(count,v.length-from);
        for (int i = 0;i<count;i++)
            sum+=v[i+from];
        return sum/count;
    }

    public static double diviation(double[] v, double avg){
        double sum = 0;
        for (int i = 0;i<v.length;i++) {
            double vv = v[i] - avg;
            sum+= vv*vv;
        }
        return Math.sqrt(sum/v.length);

    }


    /****************
     * @param coll an ArrayList of Comparable objects
     * @return the median of coll
     *****************/

    public static double median(double[] coll, int offset, int length) {
        double result;
        int n = length/2;

        if (coll.length % 2 == 0)  // even number of items; find the middle two and average them
            result = (nth(coll, offset, length,n-1) + nth(coll, offset, length, n)) / 2.0;
        else                      // odd number of items; return the one in the middle
            result = nth(coll, offset, length, n);

        return result;
    } // median(coll)



    /*****************
     * @param coll a collection of Comparable objects
     * @param n  the position of the desired object, using the ordering defined on the list elements
     * @return the nth smallest object
     *******************/

    public static double nth(double[] coll, int offset, int length, int n) {
        if (n==0) return coll[offset];
        double result, pivot;
        ArrayList<Double> underPivot = new ArrayList<>(), overPivot = new ArrayList<>(), equalPivot = new ArrayList<>();

        // choosing a pivot is a whole topic in itself.
        // this implementation uses the simple strategy of grabbing something from the middle of the ArrayList.

        pivot = coll[offset+n/2];

        // split coll into 3 lists based on comparison with the pivot

        for (int i = 0;i<length;i++) {
            double obj = coll[offset+i];

            if (obj<pivot)        // obj < pivot
                underPivot.add(obj);
            else if (obj>pivot)   // obj > pivot
                overPivot.add(obj);
            else                  // obj = pivot
                equalPivot.add(obj);
        } // for each obj in coll

        // recurse on the appropriate list

        if (n < underPivot.size())
            result = nth(underPivot, n);
        else if (n < underPivot.size() + equalPivot.size()) // equal to pivot; just return it
            result = pivot;
        else  // everything in underPivot and equalPivot is too small.  Adjust n accordingly in the recursion.
            result = nth(overPivot, n - underPivot.size() - equalPivot.size());

        return result;
    } // nth(coll, n)

    public static double nth(ArrayList<Double> coll, int n) {
        double result, pivot;
        ArrayList<Double> underPivot = new ArrayList<>(), overPivot = new ArrayList<>(), equalPivot = new ArrayList<>();

        // choosing a pivot is a whole topic in itself.
        // this implementation uses the simple strategy of grabbing something from the middle of the ArrayList.

        pivot = coll.get(n/2);

        // split coll into 3 lists based on comparison with the pivot

        for (double obj : coll) {
            if (obj < pivot)        // obj < pivot
                underPivot.add(obj);
            else if (obj > pivot)   // obj > pivot
                overPivot.add(obj);
            else                  // obj = pivot
                equalPivot.add(obj);
        } // for each obj in coll

        // recurse on the appropriate list

        if (n < underPivot.size())
            result = nth(underPivot, n);
        else if (n < underPivot.size() + equalPivot.size()) // equal to pivot; just return it
            result = pivot;
        else  // everything in underPivot and equalPivot is too small.  Adjust n accordingly in the recursion.
            result = nth(overPivot, n - underPivot.size() - equalPivot.size());

        return result;
    } // nth(coll, n)

    public static double lerped(double[] v, double pos){
        if (pos<=0) return v[0];
        if (pos>=v.length-1) return v[v.length-1];
        double v1 = v[(int)pos];
        double v2 = v[1+(int)pos];
        double k = pos-(int)pos;
        return v1*(1-k)+v2*k;
    }

    public static double[] resize(double[] v, int newLength) {
        double[] r = new double[newLength];
        if (r.length>=v.length) {
            for (int i = 0; i < r.length; i++) {
                double p = i * 1.0 * (v.length - 1) / (r.length - 1);
                r[i] = lerped(v, p);
            }
        } else {
            for (int i = 0;i<r.length;i++){
                int from = i*(v.length-1)/r.length;
                int to = (i+1)*(v.length-1)/r.length;
                double sum = 0;
                for (int j = from;j<to;j++)
                    sum+=v[j];
                r[i] = sum/(to-from);
            }
        }
        return r;
    }

    public static double[] ma(double[] v, int window){
        double[] r = new double[v.length];
        for (int i = 0;i<r.length;i++){
            double sum = 0;
            int cc = 0;
            for (int j = -window/2;j<=window/2;j++) if (i+j>=0 && i+j<v.length){
                sum+=v[i+j];
                cc++;
            }
            r[i] = sum/cc;
        }
        return r;
    }

    public static Pair<Double,Double> minMax(double[] v){
        double min = v[0];
        double max = v[0];
        for (int i = 1;i<v.length;i++){
            double vv = v[i];
            if (min>vv) min = vv;
            if (max<vv) max = vv;
        }
        return new Pair<>(min,max);
    }

    public static double[] ema(double[] v, int t) {
        double[] res = new double[v.length];
        double k = 2.0/(t+1);
        res[0] = v[0];
        for (int i = 1;i<res.length;i++)
            res[i] = (v[i]-res[i-1])*k+res[i-1];

        return res;
    }

    public static Pair<double[],double[]> emaAndDisp(double[] v, int t) {
        double[] ema = new double[v.length];
        double[] disp = new double[v.length];
        double k = 2.0/(t+1);
        ema[0] = v[0];
        disp[0] = 0;
        for (int i = 1;i<v.length;i++) {
            ema[i] = (v[i] - ema[i - 1]) * k + ema[i - 1];
            double d = v[i]-ema[i];
            d*=d;
            disp[i] = (d-disp[i-1])*k + disp[i-1];
        }
        for (int i = 1;i<v.length;i++)
            disp[i] = Math.sqrt(disp[i]);

        return new Pair<>(ema,disp);
    }

    public static Pair<double[],double[]> gustosEmaAndDisp(double[] v, int t, double[] volumes, int volumeT) {
        double[] ema = new double[v.length];
        double[] disp = new double[v.length];
        ema[0] = v[0];
        disp[0] = 0;
        double prevVolume = volumes[0];
        for (int i = 1;i<v.length;i++) {
            double vol = (volumes[i]-prevVolume)*2.0/(volumeT+1) + prevVolume;
            prevVolume = vol;
            double vk = volumes[i]/vol;

            ema[i] = (v[i] - ema[i - 1]) * 2.0/(t/vk+1) + ema[i - 1];
            double d = v[i]-ema[i];
            d*=d;
            disp[i] = (d-disp[i-1])*2.0/(t+1) + disp[i-1];
        }
        for (int i = 1;i<v.length;i++)
            disp[i] = Math.sqrt(disp[i]);

        return new Pair<>(ema,disp);
    }

    public static Pair<double[],double[]> gustosEmaAndDisp2(double[] v, int t, double[] volumes, int volumeT) {
        double[] ema = new double[v.length];
        double[] disp = new double[v.length];
        double avgVolume = VecUtils.avg(volumes);
        ema[0] = v[0];
        disp[0] = 0;
        double prevVolume = volumes[0];
        for (int i = 1;i<v.length;i++) {
            double vk = volumes[i]/avgVolume;

            ema[i] = (v[i] - ema[i - 1]) * 2.0/(t/vk+1) + ema[i - 1];
            double d = v[i]-ema[i];
            d*=d;
            disp[i] = (d-disp[i-1])*2.0/(t/vk+1) + disp[i-1];
        }
        for (int i = 1;i<v.length;i++)
            disp[i] = Math.sqrt(disp[i]);

        return new Pair<>(ema,disp);
    }

    public static Pair<double[],double[]> mcginleyAndDisp(double[] v, int t) {
        double[] mc = new double[v.length];
        double[] disp = new double[v.length];
        double k = 2.0/(t+1);
        mc[0] = v[0];
        disp[0] = 0;
        for (int i = 1;i<v.length;i++) {
            double a = v[i] / mc[i - 1];
            a*=a;
            a*=a;
            mc[i] = mc[i-1]+(v[i]-mc[i-1])/(0.6*t*a);
            double d = Math.abs(v[i]-mc[i]);
            d=d*d;
//            a = d / Math.max(1,disp[i - 1]);
//            a*=a;
//            disp[i] = disp[i-1]+(d-disp[i-1])/(0.6*t*a);
            disp[i] = (d-disp[i-1])*k + disp[i-1];

        }
        for (int i = 1;i<v.length;i++)
            disp[i] = Math.sqrt(disp[i]);

        return new Pair<>(mc,disp);
    }

    public static Pair<double[],double[]> gustosMcginleyAndDisp(double[] v, int t, double[] volumes, int volT) {
        double[] mc = new double[v.length];
        double[] disp = new double[v.length];
        double[] volumesAvg = ema(volumes,volT);
        mc[0] = v[0];
        disp[0] = 0;
        for (int i = 1;i<v.length;i++) {
            double a = v[i] / mc[i - 1];
            double volumek = volumes[i]/Math.max(1,volumesAvg[i]);
            a*=a;
            a*=a;
            double next = mc[i - 1] + (v[i] - mc[i - 1]) / (0.6 * t * a);
            if (volumek<=1) {
                volumek = Math.pow(volumek, 5);
                mc[i] = mc[i - 1] * (1 - volumek) + next * volumek;
            } else {
                double vk = volumek;
                double pn = 0;
                while (vk>1) {
                    pn = next;
                    next = next + (v[i] - next) / (0.6 * t * a);
                    vk-=1;
                }
                mc[i] = pn*(1-volumek)+next*volumek;

            }
            double d = Math.abs(v[i]-mc[i]);
            d=d*d;
            disp[i] = (d-disp[i-1])* 2.0/(t/volumek+1) + disp[i-1];

        }
        for (int i = 1;i<v.length;i++)
            disp[i] = Math.sqrt(disp[i]);

        return new Pair<>(mc,disp);
    }

    public static Pair<double[],double[]> emaAndMed(double[] v, int t) {
        double[] ema = new double[v.length];
        double[] temp = new double[v.length];
        double[] disp = new double[v.length];
        double k = 2.0/(t+1);
        ema[0] = v[0];
        temp[0] = 0;
        for (int i = 1;i<v.length;i++) {
            ema[i] = (v[i] - ema[i - 1]) * k + ema[i - 1];
            double d = v[i]-ema[i];
            temp[i] = Math.abs(d);
        }
        for (int i = t;i<v.length;i++)
            disp[i] = median(temp,i-t,t);

        return new Pair<>(ema,disp);
    }

    public static double[] disp(double[] v, double[] avg, int t) {
        double[] res = new double[v.length];
        for (int i = t;i<res.length;i++){
            double a = avg[i];
            double s = 0;
            for (int j = 0;j<t;j++){
                double d = v[i - j] - a;
                s+= d*d;
            }
            res[i] = Math.sqrt(s/t);
        }

        return res;
    }

    public static void toFile(String path, double[] v) throws Exception {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(path))){
            out.writeInt(v.length);
            for (int i = 0;i<v.length;i++){
                out.writeDouble(v[i]);
            }
        }
    }

    public static double[] fromFile(String path) throws Exception{
        try (DataInputStream in = new DataInputStream(new FileInputStream(path))){
            double[] v = new double[in.readInt()];
            for (int i = 0;i<v.length;i++)
                v[i] = in.readDouble();
            return v;
        }
    }

    public static double[] add(double[] v1, double[] v2, int k) {
        double[] res = new double[v1.length];
        for (int i = 0;i<v1.length;i++)
            res[i] = v1[i]+v2[i]*k;
        return res;
    }
}
