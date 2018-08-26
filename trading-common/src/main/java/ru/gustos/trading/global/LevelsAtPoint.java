package ru.gustos.trading.global;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import ru.gustos.trading.book.BarsSource;
import ru.gustos.trading.book.Sheet;

import java.util.ArrayList;

public class LevelsAtPoint {
    public ArrayList<SimpleRegression> minlevels = new ArrayList<>();
    public ArrayList<SimpleRegression> maxlevels = new ArrayList<>();
    BarsSource sheet;
    int index;
    static int[] cnts = new int[]{10,100};

    public LevelsAtPoint(BarsSource sheet, int index, int pow) {
        this.sheet = sheet;
        this.index = index;
        findLevels(pow);
    }

    private void findLevels(int pow) {
        int[] mins = findMins(pow);
        SimpleRegression reg;
//        if (sheet.bar(mins[0]).getMinPrice()>sheet.bar(mins[1]).getMinPrice()){
            reg = new SimpleRegression();
            reg.addData(mins[1],sheet.bar(mins[1]).getMinPrice());
            reg.addData(mins[0],sheet.bar(mins[0]).getMinPrice());
            minlevels.add(reg);
//        }

        int[] maxs = findMaxs(pow);
//        if (sheet.bar(maxs[0]).getMaxPrice()<sheet.bar(maxs[1]).getMaxPrice()){
            reg = new SimpleRegression();
            reg.addData(maxs[1],sheet.bar(maxs[1]).getMaxPrice());
            reg.addData(maxs[0],sheet.bar(maxs[0]).getMaxPrice());
            maxlevels.add(reg);
//        }

    }

    public int[] findMins(int pow) {
        int i = index-pow*2/3;
        int[] res = new int[2];
        int ii = 0;
        while (i>=0 && ii<2){
            if (isMin(i,pow)){
                res[ii++] = i;
            }
            i--;
        }
        return res;
    }

    public int[] findMaxs(int pow) {
        int i = index-pow*2/3;
        int[] res = new int[2];
        int ii = 0;
        while (i>=0 && ii<2){
            if (isMax(i,pow)){
                res[ii++] = i;
            }
            i--;
        }
        return res;
    }

    public boolean isMin(int pos, int pow) {
        double m = sheet.bar(pos).getMinPrice();
        for (int i = Math.max(0, pos - pow); i <= Math.min(index, pos + pow); i++)
            if (i != pos)
                if (sheet.bar(i).getMinPrice() < m)
                    return false;
        return true;

    }

    public boolean isMax(int pos, int pow) {
        double m = sheet.bar(pos).getMaxPrice();
        for (int i = Math.max(0, pos - pow); i <= Math.min(index, pos + pow); i++)
            if (i != pos)
                if (sheet.bar(i).getMaxPrice() > m)
                    return false;
        return true;

    }


}
