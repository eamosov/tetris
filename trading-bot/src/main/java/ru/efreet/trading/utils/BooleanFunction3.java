package ru.efreet.trading.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fluder on 26/02/2018.
 */
public class BooleanFunction3 {

    static List<Object[]> table = new ArrayList<>();

    static {
        List<boolean[]> sell = new ArrayList<>();
        gen(sell, new boolean[8], 0);

        List<boolean[]> buy = new ArrayList<>();
        gen(buy, new boolean[8], 0);

        for (boolean[] s : sell) {
            for (boolean[] b : buy) {
                if (allow(s, b) && notConst(s) && notConst(b)) {
                    table.add(new Object[]{b,s});
                }
            }
        }
    }

    private static boolean[] copy(boolean[] src) {
        final boolean dst[] = new boolean[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    private static void gen(List<boolean[]> result, boolean[] src, int index) {

        if (index >= src.length) {
            result.add(copy(src));
        } else {
            src[index] = false;
            gen(result, src, index + 1);
            src[index] = true;
            gen(result, src, index + 1);
        }
    }

    //Исключаются варианты, которые одновременно возвращают true на BUY и SELL
    private static boolean allow(boolean s[], boolean b[]) {
        for (int i = 0; i < s.length; i++) {
            if (s[i] == true && b[i] == true) {
                return false;
            }
        }
        return true;
    }

    private static boolean alwaysTrue(boolean s[]) {
        for (int i = 0; i < s.length; i++) {
            if (s[i] == false) {
                return false;
            }
        }

        return true;
    }

    private static boolean alwaysFalse(boolean s[]) {
        for (int i = 0; i < s.length; i++) {
            if (s[i] == true) {
                return false;
            }
        }
        return true;
    }

    //искулючаются варианты, дающие всегда true или false
    private static boolean notConst(boolean s[]){
        return alwaysFalse(s) == false && alwaysTrue(s) == false;
    }

    private static int toInt(boolean v){
        return v == true ? 1 : 0;
    }

    public static boolean get(int function, boolean buy,  boolean i, boolean j, boolean k){
        return ((boolean[])table.get(function)[buy ? 0 : 1])[4*toInt(i) + 2 *toInt(j) + toInt(k) ];
    }

    public static int size(){
        return table.size();
    }

    public static void main(String[] args) {

        System.out.println(BooleanFunction3.size());

        System.out.println(BooleanFunction3.get(0, true, false, false, false));

    }
}
