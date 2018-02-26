package ru.efreet.trading.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fluder on 26/02/2018.
 */
public class IntFunction3 {

    static List<int[]> table = new ArrayList<>();

    static {
        gen(table, new int[12], 0);
    }

    private static int[] copy(int[] src) {
        final int dst[] = new int[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    private static void gen(List<int[]> result, int[] src, int index) {

        if (index >= src.length) {
            result.add(copy(src));
        } else {
            src[index] = 0;
            gen(result, src, index + 1);
            src[index] = 1;
            gen(result, src, index + 1);
            src[index] = 2;
            gen(result, src, index + 1);
        }
    }


    public static int get(int function, int sd, int m1, int m2) {
        return table.get(function)[4 * sd + 2 * m2 + m2];
    }

    public static int size() {
        return table.size();
    }
}
