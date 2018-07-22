package ru.gustos.trading.book.indicators;

import kotlin.Pair;
import ru.efreet.trading.bars.XBaseBar;
import ru.gustos.trading.book.Volumes;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VecUtils {


    public static double avg(double[] v) {
        double sum = 0;
        for (int i = 0; i < v.length; i++)
            sum += v[i];
        return sum / v.length;
    }

    public static double avg(double[] v, int from, int count) {
        double sum = 0;
        count = Math.min(count, v.length - from);
        for (int i = 0; i < count; i++)
            sum += v[i + from];
        return sum / count;
    }

    public static double diviation(double[] v, double avg) {
        double sum = 0;
        for (int i = 0; i < v.length; i++) {
            double vv = v[i] - avg;
            sum += vv * vv;
        }
        return Math.sqrt(sum / v.length);

    }


    /****************
     * @param coll an ArrayList of Comparable objects
     * @return the median of coll
     *****************/

    public static double median(double[] coll, int offset, int length) {
        double result;
        int n = length / 2;

        if (coll.length % 2 == 0)  // even number of items; find the middle two and average them
            result = (nth(coll, offset, length, n - 1) + nth(coll, offset, length, n)) / 2.0;
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
        if (n == 0) return coll[offset];
        double result, pivot;
        ArrayList<Double> underPivot = new ArrayList<>(), overPivot = new ArrayList<>(), equalPivot = new ArrayList<>();

        // choosing a pivot is a whole topic in itself.
        // this implementation uses the simple strategy of grabbing something from the middle of the ArrayList.

        pivot = coll[offset + n / 2];

        // split coll into 3 lists based on comparison with the pivot

        for (int i = 0; i < length; i++) {
            double obj = coll[offset + i];

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

    public static double nth(ArrayList<Double> coll, int n) {
        double result, pivot;
        ArrayList<Double> underPivot = new ArrayList<>(), overPivot = new ArrayList<>(), equalPivot = new ArrayList<>();

        // choosing a pivot is a whole topic in itself.
        // this implementation uses the simple strategy of grabbing something from the middle of the ArrayList.

        pivot = coll.get(n / 2);

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

    public static double lerped(double[] v, double pos) {
        if (pos <= 0) return v[0];
        if (pos >= v.length - 1) return v[v.length - 1];
        double v1 = v[(int) pos];
        double v2 = v[1 + (int) pos];
        double k = pos - (int) pos;
        return v1 * (1 - k) + v2 * k;
    }

    public static double[] resize(double[] v, int newLength) {
        double[] r = new double[newLength];
        if (r.length >= v.length) {
            for (int i = 0; i < r.length; i++) {
                double p = i * 1.0 * (v.length - 1) / (r.length - 1);
                r[i] = lerped(v, p);
            }
        } else {
            for (int i = 0; i < r.length; i++) {
                int from = i * (v.length - 1) / r.length;
                int to = (i + 1) * (v.length - 1) / r.length;
                double sum = 0;
                for (int j = from; j < to; j++)
                    sum += v[j];
                r[i] = sum / (to - from);
            }
        }
        return r;
    }

    public static double[] ma(double[] v, int window) {
        if (window / 2 < 1)
            return v.clone();
        double[] r = new double[v.length];
        for (int i = 0; i < r.length; i++) {
            double sum = 0;
            int cc = 0;
            for (int j = -window / 2; j <= window / 2; j++)
                if (i + j >= 0 && i + j < v.length) {
                    sum += v[i + j];
                    cc++;
                }
            r[i] = sum / cc;
        }
        return r;
    }

    public static boolean isLocalMinimum(double[] v, int pos) {
        int p = pos - 1;
        while (p >= 0 && v[p] == v[pos]) p--;
        if (p >= 0 && v[p] < v[pos]) return false;
        p = pos + 1;
        while (p < v.length && v[p] == v[pos]) p++;
        if (p < v.length && v[p] < v[pos]) return false;
        return true;
    }

    public static boolean isLocalMaximum(double[] v, int pos) {
        int p = pos - 1;
        while (p >= 0 && v[p] == v[pos]) p--;
        if (p >= 0 && v[p] > v[pos]) return false;
        p = pos + 1;
        while (p < v.length && v[p] == v[pos]) p++;
        if (p < v.length && v[p] > v[pos]) return false;
        return true;
    }


    public static Pair<Double, Double> minMax(double[] v) {
        double min = v[0];
        double max = v[0];
        for (int i = 1; i < v.length; i++) {
            double vv = v[i];
            if (min > vv) min = vv;
            if (max < vv) max = vv;
        }
        return new Pair<>(min, max);
    }

    public static Pair<Double, Double> minMax(double[] v, Pair<Double, Double> minMax) {
        double min = minMax.getFirst();
        double max = minMax.getSecond();
        for (int i = 1; i < v.length; i++) {
            double vv = v[i];
            if (min > vv) min = vv;
            if (max < vv) max = vv;
        }
        return new Pair<>(min, max);
    }

    public static void minMax(double[] v, double[] min, double[] max) {
        for (int i = 0; i < v.length; i++) {
            double vv = v[i];
            if (min[i] > vv) min[i] = vv;
            if (max[i] < vv) max[i] = vv;
        }
    }

    public static double[] ema(double[] v, int t) {
        double[] res = new double[v.length];
        double k = 2.0 / (t + 1);
        res[0] = v[0];
        for (int i = 1; i < res.length; i++)
            res[i] = (v[i] - res[i - 1]) * k + res[i - 1];

        return res;
    }

    public static Pair<double[], double[]> emaAndDisp(double[] v, int t) {
        double[] ema = new double[v.length];
        double[] disp = new double[v.length];
        double k = 2.0 / (t + 1);
        ema[0] = v[0];
        disp[0] = 0;
        for (int i = 1; i < v.length; i++) {
            ema[i] = (v[i] - ema[i - 1]) * k + ema[i - 1];
            double d = v[i] - ema[i];
            d *= d;
            disp[i] = (d - disp[i - 1]) * k + disp[i - 1];
        }
        for (int i = 1; i < v.length; i++)
            disp[i] = Math.sqrt(disp[i]);

        return new Pair<>(ema, disp);
    }

    public static Pair<double[], double[]> gustosEmaAndDisp(double[] v, int t, double[] volumes, int volumeT) {
        double[] ema = new double[v.length];
        double[] disp = new double[v.length];
        ema[0] = v[0];
        disp[0] = 0;
        double prevVolume = volumes[0];
        for (int i = 1; i < v.length; i++) {
            double vol = (volumes[i] - prevVolume) * 2.0 / (volumeT + 1) + prevVolume;
            prevVolume = vol;
            double vk = volumes[i] / vol;

            ema[i] = (v[i] - ema[i - 1]) * 2.0 / (t / vk + 1) + ema[i - 1];
            double d = v[i] - ema[i];
            d *= d;
            disp[i] = (d - disp[i - 1]) * 2.0 / (t + 1) + disp[i - 1];
        }
        for (int i = 1; i < v.length; i++)
            disp[i] = Math.sqrt(disp[i]);

        return new Pair<>(ema, disp);
    }

    public static Pair<double[], double[]> gustosEmaAndDisp2(double[] v, int t, double[] volumes, int volumeT) {
        double[] ema = new double[v.length];
        double[] disp = new double[v.length];
        double avgVolume = VecUtils.avg(volumes);
        ema[0] = v[0];
        disp[0] = 0;
        double prevVolume = volumes[0];
        for (int i = 1; i < v.length; i++) {
            double vk = volumes[i] / avgVolume;

            ema[i] = (v[i] - ema[i - 1]) * 2.0 / (t / vk + 1) + ema[i - 1];
            double d = v[i] - ema[i];
            d *= d;
            disp[i] = (d - disp[i - 1]) * 2.0 / (t / vk + 1) + disp[i - 1];
        }
        for (int i = 1; i < v.length; i++)
            disp[i] = Math.sqrt(disp[i]);

        return new Pair<>(ema, disp);
    }

    public static Pair<double[], double[]> mcginleyAndDisp(double[] v, int t) {
        double[] mc = new double[v.length];
        double[] disp = new double[v.length];
        double k = 2.0 / (t + 1);
        mc[0] = v[0];
        disp[0] = 0;
        for (int i = 1; i < v.length; i++) {
            double a = v[i] / mc[i - 1];
            a *= a;
            a *= a;
            mc[i] = mc[i - 1] + (v[i] - mc[i - 1]) / (0.6 * t * a);
            double d = Math.abs(v[i] - mc[i]);
            d = d * d;
//            a = d / Math.max(1,disp[i - 1]);
//            a*=a;
//            disp[i] = disp[i-1]+(d-disp[i-1])/(0.6*t*a);
            disp[i] = (d - disp[i - 1]) * k + disp[i - 1];

        }
        for (int i = 1; i < v.length; i++)
            disp[i] = Math.sqrt(disp[i]);

        return new Pair<>(mc, disp);
    }

    public static Pair<double[], double[]> gustosMcginleyAndDisp(double[] v, int t, double[] volumes, int volT) {
        double[] mc = new double[v.length];
        double[] disp = new double[v.length];
        double[] volumesAvg = ema(volumes, volT);
        mc[0] = v[0];
        disp[0] = 0;
        for (int i = 1; i < v.length; i++) {
            double a = v[i] / mc[i - 1];
            double volumek = volumes[i] / Math.max(1, volumesAvg[i]);
            a *= a;
            a *= a;
            double next = mc[i - 1] + (v[i] - mc[i - 1]) / (0.6 * t * a);
            if (volumek <= 0) {
                mc[i] = mc[i - 1];
            }
            if (volumek <= 1) {
                volumek = Math.pow(volumek, 5);
                mc[i] = mc[i - 1] * (1 - volumek) + next * volumek;
            } else {
                double vk = Math.pow(volumek, 2.3);
                double pn = 0;
                while (vk > 1) {
                    pn = next;
                    next = next + (v[i] - next) / (0.6 * t * a);
                    vk -= 1;
                }

                mc[i] = pn + vk * (next - pn);

            }
            double d = Math.abs(v[i] - mc[i]);
            d = d * d;
            disp[i] = (d - disp[i - 1]) * 2.0 / (t / volumek + 1) + disp[i - 1];

        }
        for (int i = 1; i < v.length; i++)
            disp[i] = Math.sqrt(disp[i]);

        return new Pair<>(mc, disp);
    }

    public static Pair<double[], double[]> emaAndMed(double[] v, int t) {
        double[] ema = new double[v.length];
        double[] temp = new double[v.length];
        double[] disp = new double[v.length];
        double k = 2.0 / (t + 1);
        ema[0] = v[0];
        temp[0] = 0;
        for (int i = 1; i < v.length; i++) {
            ema[i] = (v[i] - ema[i - 1]) * k + ema[i - 1];
            double d = v[i] - ema[i];
            temp[i] = Math.abs(d);
        }
        for (int i = t; i < v.length; i++)
            disp[i] = median(temp, i - t, t);

        return new Pair<>(ema, disp);
    }

    public static double[] disp(double[] v, double[] avg, int t) {
        double[] res = new double[v.length];
        for (int i = t; i < res.length; i++) {
            double a = avg[i];
            double s = 0;
            for (int j = 0; j < t; j++) {
                double d = v[i - j] - a;
                s += d * d;
            }
            res[i] = Math.sqrt(s / t);
        }

        return res;
    }

    public static void toFile(String path, double[] v) throws Exception {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(path))) {
            out.writeInt(v.length);
            for (int i = 0; i < v.length; i++) {
                out.writeDouble(v[i]);
            }
        }
    }

    public static double[] fromFile(String path) throws Exception {
        try (DataInputStream in = new DataInputStream(new FileInputStream(path))) {
            double[] v = new double[in.readInt()];
            for (int i = 0; i < v.length; i++)
                v[i] = in.readDouble();
            return v;
        }
    }

    public static double[] add(double[] v1, double[] v2, int k) {
        double[] res = new double[v1.length];
        for (int i = 0; i < v1.length; i++)
            res[i] = v1[i] + v2[i] * k;
        return res;
    }

    public static XBaseBar expandMinMax(XBaseBar minMax, double[] avg, double[] disp, double k, int from, int cnt) {
        XBaseBar res = new XBaseBar(minMax);
        int to = Math.min(from + cnt, avg.length);
        if (disp != null) {
            for (int i = from; i < to; i++) {
                res.setMinPrice(Math.min(res.getMinPrice(), avg[i] - disp[i] * k));
                res.setMaxPrice(Math.max(res.getMaxPrice(), avg[i] + disp[i] * k));
            }
        } else
            for (int i = from; i < to; i++)
                expandMinMax(minMax,avg[i]);
        return res;
    }

    public static XBaseBar expandMinMax(XBaseBar minMax, double price){
        minMax.setMinPrice(Math.min(minMax.getMinPrice(), price));
        minMax.setMaxPrice(Math.max(minMax.getMaxPrice(), price));
        return minMax;
    }

    public static Pair<double[], double[]> futureMaAndDisp(double[] v, int window) {
        double[] res = new double[v.length];
        double[] disp = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            double sum = 0;
            int from = Math.max(0, i - window / 2);
            int to = Math.min(i + window / 2, v.length - 1);
            for (int j = from; j <= to; j++)
                sum += v[j];

            res[i] = sum / (to - from + 1);
        }
        for (int i = 0; i < v.length; i++) {
            double sum = 0;
            int from = Math.max(0, i - window / 2);
            int to = Math.min(i + window / 2, v.length - 1);
            for (int j = from; j <= to; j++) {
                double d = v[j] - res[j];
                sum += d * d;
            }
            disp[i] = Math.sqrt(sum / (to - from + 1));
        }
        return new Pair<>(res, disp);
    }

    public static void mul(double[] vv, double k) {
        for (int i = 0; i < vv.length; i++)
            vv[i] *= k;
    }

    public static double sum(double[] vv) {
        double res = 0;
        for (int i = 0; i < vv.length; i++)
            res += vv[i];
        return res;
    }

    public static int findMaximum(double[] v, int pos, int dir) {
        int p = pos;
        while (p >= 0 && p < v.length && !isLocalMaximum(v, p))
            p += dir;
        if (p < 0 || p >= v.length) return -1;
        return p;
    }

    public static int findMinimum(double[] v, int pos, int dir) {
        int p = pos;
        while (p >= 0 && p < v.length && !isLocalMinimum(v, p))
            p += dir;
        if (p < 0 || p >= v.length) return -1;
        return p;
    }

    public static double findMaximumValue(double[] v, int pos, int dir) {
        int p = pos;
        double max = v[pos];
        while (p >= 0 && p < v.length && v[p]>=max) {
            p += dir;
            max = Math.max(v[p],max);

        }
        return max;
    }

    public static double findMinimumValue(double[] v, int pos, int dir) {
        int p = pos;
        double min = v[pos];
        while (p >= 0 && p < v.length && v[p]<=min) {
            p += dir;
            min = Math.min(v[p],min);

        }
        return min;
    }

    public static int goDownToMinimum(double[] v, int pos) {
        if (isLocalMinimum(v, pos)) return pos;
        int p = pos - 1;
        while (p >= 0 && v[p] == v[pos]) p--;
        int dir = p < 0 || v[p] > v[pos] ? 1 : -1;
        p = pos;
        while (p + dir >= 0 && p + dir < v.length && v[p] >= v[p + dir]) p += dir;
        if (p < 0) return 0;
        if (p >= v.length) return v.length - 1;
        return p;
    }

    public static int goToChange(double[] v, int pos, int dir, double eps) {
//        if (true) return pos+dir*3;
        int p = pos;
        do {
            p += dir;
        } while (p >= 0 && p < v.length && v[p] == v[pos]);
        if (p < 0 || p >= v.length) return p;
        double sign = v[p] - v[pos];

        int lastP = p;
        boolean signok;
        while (p + dir >= 0 && p + dir < v.length && ((signok = sign * (v[p + dir] - v[p]) >= 0) || Math.abs(v[p + dir] / v[lastP] - 1) < eps)) {
            p += dir;
            if (signok)
                lastP = p;
        }
//        while (p+dir>=0 && p+dir<v.length && (sign*(v[p+dir]-v[p])>=0 || Math.abs(v[p+dir]/v[p]-1)<0.05)) {
//            p += dir;
//        }


        return lastP;
    }

    public static double[] dif(double[] v) {
        double[] res = v.clone();
        for (int i = 1; i < res.length; i++)
            res[i] = v[i] - v[i - 1];
        res[0] = 0;
        return res;
    }

    public static int goToChange2(double[] v, int pos, int dir, double eps) {
        int p = pos + dir;

        while (p + dir >= 0 && p + dir < v.length && Math.abs((v[p + dir] - v[p]) / Math.max(0.0001, Math.abs(v[p] - v[p - dir]))) < eps) {
            p += dir;
        }


        return p;
    }

    private static boolean changeInEpsilon(double v1, double v2, double eps) {
        if (v1 == 0 && v2 == 0) return true;
        if (v1 == 0 || v2 == 0) return false;
        return Math.abs(v1 / v2 - 1) < eps;
    }

    public static int goToChange3(double[] v, int pos, int dir, double eps) {
        int p = pos + dir;
        double sum = 0;
        int cnt = 0;
        boolean in = changeInEpsilon(v[p], v[p - dir], eps);
        while (p >= 0 && p < v.length) {
            boolean in2 = changeInEpsilon(v[p], v[p - dir], eps);
            if (in2 != in) return p;
            p += dir;
        }
        return p;


    }

    public static ArrayList<Double> integrals = new ArrayList<>();
    public static int[] listLevels(double[] v, double k, double k2) {
        v = VecUtils.makeDifForLevels(v,k, k2);
        integrals.clear();
        int p = 0;
        List<Integer> l = new ArrayList<>();
        double sum = 0;
        do {
            sum+=v[p];
            p++;
            if (v[p-1]!=0 && v[p]==0) {
                l.add(p - 1);
                integrals.add(sum);
                sum = 0;
            }else if (v[p-1]*v[p]<0) {
                l.add(Math.abs(v[p - 1]) < Math.abs(v[p]) ? p - 1 : p);
                integrals.add(sum);
                sum = 0;
            }


            else if (v[p-1]==0 && v[p]!=0) {
                if (l.size()==0 || p-l.get(l.size()-1)>10) {
                    l.add(p);
                    integrals.add(sum);
                    sum = 0;
                }else
                    l.set(l.size()-1,(l.get(l.size()-1)+p)/2);
            }

            if (p >= v.length-1) break;
        } while (true);
        integrals.add(0.0);
        integrals.add(0.0);
        integrals.add(0.0);
//        v = dif(v);
//        v = dif(v);
//        for (int i = 1;i<v.length;i++)
//            if (Math.abs(v[i])<5)
//                v[i] = 0;
//        for (int i = 1;i<v.length;i++)
//            if ((v[i]==0) != (v[i-1]==0)) l.add(i);
//        l.sort(Integer::compare);
        return l.stream().mapToInt(Integer::intValue).toArray();
    }

    public static double[] hairFilter(double[] v) {
        double[] r = v.clone();
        for (int i = 1; i < v.length - 1; i++) {
            if (v[i] > v[i - 1] && v[i] > v[i + 1])
                r[i] = (v[i - 1] + v[i + 1]) / 2;
        }
        return r;
    }

    public static double[] negativeHairFilter(double[] v) {
        double[] r = v.clone();
        for (int i = 1; i < v.length - 1; i++) {
            if (v[i] < v[i - 1] && v[i] < v[i + 1])
                r[i] = (v[i - 1] + v[i + 1]) / 2;
        }
        return r;
    }

    public static double[] removeSmallsOnDif(double[] v, double eps) {
        double[] r = v.clone();
        int prevChange = 0;
        double sum = 0;
        for (int i = 1; i < v.length - 1; i++) {
            if (v[i] * v[i - 1] <= 0) {
                if (Math.abs(sum) < eps) {
                    Arrays.fill(r, prevChange, i, 0);
                }
                prevChange = i;
                sum = 0;
            }
            sum += v[i]*v[i];
        }
        return r;
    }

    public static double[] pitsToZero(double[] v, double k) {
        double[] r = v.clone();
        for (int i = 5; i < v.length - 5; i++)
            if (v[i] > 0) {
                if (v[i] < v[i - 1] && v[i] < v[i + 1] && v[i]*k<findMaximumValue(v,i,-1) && v[i]*k<findMaximumValue(v,i,1))
                    r[i] = 0;
            } else if (v[i]<0){
                if (v[i] > v[i - 1] && v[i] > v[i + 1] && v[i]*k>findMinimumValue(v,i,-1) && v[i]*k>findMinimumValue(v,i,1))
                    r[i] = 0;
            }
            return r;

    }


    public static double[] smallToZero(double[] v, double eps) {
        double[] r = v.clone();
        for (int i = 0;i<v.length;i++)
            if (Math.abs(v[i])<eps)
                r[i] = 0;
        return r;
    }

    public static double[] abs(double[] v) {
        return Arrays.stream(v).map(Math::abs).toArray();
    }

    public static double[] makeDifForLevels(double[] data, double k, double k2) {
        double[] d = VecUtils.ma(VecUtils.hairFilter(VecUtils.hairFilter(data)),(int)(2*k2));
        double[] dif = VecUtils.dif(d);
        dif = VecUtils.ma(dif,(int)k2);
        dif = VecUtils.hairFilter(VecUtils.hairFilter(dif));
        dif = VecUtils.negativeHairFilter(VecUtils.negativeHairFilter(dif));
        double[] difLongMa = VecUtils.ma(VecUtils.abs(dif), 100);
//        double maxdif = Arrays.stream(dif).map(Math::abs).max().getAsDouble();
        dif = VecUtils.div(dif,difLongMa);
//        dif = VecUtils.pitsToZero(dif,2);
//        dif = VecUtils.smallToZero(dif,0.4);
        dif = VecUtils.removeSmallsOnDif(dif,2*k);
        return dif;
    }

    public static double[] div(double[] v, double[] v2) {
        double[] r = v.clone();
        for (int i = 0;i<r.length;i++)
            r[i] = v2[i]==0?0:v[i]/v2[i];
        return r;
    }

    public static int findBaseInLevels(int[] levels, int current){
        for (int i = 0;i<levels.length;i++)
            if (levels[i]>current)
                return i==0?0:i-1;
        return levels.length-1;
    }

    public static int nextLevel(int[] levels, int fromIndex, int move, int steps){
        fromIndex += move;
        if (fromIndex<0) return Math.max(0,levels[0]+fromIndex*10);
        if (fromIndex>=levels.length) return Math.min(steps,levels[levels.length-1]+(fromIndex-levels.length+1)*10);
        return levels[fromIndex];

    }

    public static double[] norm(double[] v) {
        Pair<Double, Double> mm = VecUtils.minMax(v);
        double[] res = v.clone();
        for (int i = 0;i<res.length;i++)
            res[i] = (res[i]-mm.getFirst())/(mm.getSecond()-mm.getFirst());
        return res;
    }
}
