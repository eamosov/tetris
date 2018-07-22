package ru.gustos.trading.book.ml;

import ru.gustos.trading.global.MomentData;

import java.util.Arrays;
import java.util.Objects;

public class DataCorelation{

    public static double check(MomentData[] data, int index, int target){
        double[] v = Arrays.stream(data).filter(Objects::nonNull).mapToDouble(d -> d.values[index]).toArray();
        double[] t = Arrays.stream(data).filter(Objects::nonNull).mapToDouble(d -> d.values[target]).toArray();

        return 0;
    }
}
