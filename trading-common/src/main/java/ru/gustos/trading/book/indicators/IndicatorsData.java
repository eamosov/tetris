package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;

public class IndicatorsData {
    private HashMap<Integer,double[]> data = new HashMap<>();
    private Sheet sheet;

    public IndicatorsData(Sheet sheet){
        this.sheet = sheet;
    }

    public void calc(IIndicator ind) {
        calc(ind,0,sheet.moments.size());
    }

    public void calc(IIndicator ind, int from, int to){
        double[] values =  new double[sheet.moments.size()];
        if (from!=0) {
            double[] old = data.getOrDefault(ind.getId(), null);
            if (old != null)
                System.arraycopy(old, 0, values, 0, old.length);
        }
        ind.calcValues(sheet,values,from,to);
        data.put(ind.getId(),values);
    }

    public double get(IIndicator indicator, int index) {
        return data.get(indicator.getId())[index];
    }

    public double get(IIndicator indicator, int index,int scale) {
        double[] data = this.data.get(indicator.getId());
        if (scale==1)
            return data[index];
        double res = 0;
        for (int i = 0;i<scale;i++) {
            if (index+i>=data.length) break;
            res += data[index + i];
        }
//        res/=scale;
        return res;
    }

    public int hasYesNo(IIndicator indicator, int index,int scale) {
        int result = 0;
        double[] data = this.data.get(indicator.getId());
        if (scale==1) {
            double v = data[index];
            if (v==IIndicator.YES)
                result=1;
            else if (v==IIndicator.NO)
                result=2;
        } else {
            for (int i = 0;i<scale;i++) {
                if (index+i>=data.length) break;
                double v = data[index + i];
                if (v==IIndicator.YES)
                    result|=1;
                else if (v==IIndicator.NO)
                    result|=2;
            }
        }
        return result;
    }

    public double get(int id, int index) {
        return data.get(id)[index];
    }

    public double[] get(int id){
        return data.get(id);
    }

    public void save(IndicatorsDb db) throws SQLException {
        Integer[] keys = data.keySet().toArray(new Integer[0]);

        String[] names = Arrays.stream(keys).map(id -> sheet.getLib().get(id).getName().toLowerCase()).toArray(String[]::new);
        double[][] values =  Arrays.stream(keys).map(k->data.get(k)).toArray(double[][]::new);
        db.save(sheet,sheet.getFrom(), names,values);
    }

    public void load(IndicatorsDb db) throws SQLException {
        int[] keys = sheet.getLib().listIndicators().stream().mapToInt(IIndicator::getId).toArray();
        String[] names = Arrays.stream(keys).mapToObj(id -> sheet.getLib().get(id).getName().toLowerCase()).toArray(String[]::new);
        db.load(sheet,names,keys,data);
    }
}
