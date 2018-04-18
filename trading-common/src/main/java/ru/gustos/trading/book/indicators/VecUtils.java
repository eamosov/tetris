package ru.gustos.trading.book.indicators;

import kotlin.Pair;

import java.util.ArrayList;

public class VecUtils {


    public static double avg(double[] v){
        double sum = 0;
        for (int i = 0;i<v.length;i++)
            sum+=v[i];
        return sum/v.length;
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
}
