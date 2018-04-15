package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.util.ArrayList;


public class IndicatorUtils{

    public static int bars(IndicatorPeriod period,Sheet sheet){
        switch (period){
            case TENMINUTES:
                return 2;
            case HOUR:
                return 4;
            case DAY:
                return 6;
            case WEEK:
                return 8;
            case MONTH:
                return 10;
        }
//        switch (period){
//            case TENMINUTES:
//                return 601/(int)sheet.interval().getDuration().getSeconds();
//            case HOUR:
//                return 3601/(int)sheet.interval().getDuration().getSeconds();
//            case DAY:
//                return (3600*24+1)/(int)sheet.interval().getDuration().getSeconds();
//            case WEEK:
//                return (3600*24*7+1)/(int)sheet.interval().getDuration().getSeconds();
//            case MONTH:
//                return (3600*24*30+1)/(int)sheet.interval().getDuration().getSeconds();
//        }
        throw new NullPointerException();
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
}
