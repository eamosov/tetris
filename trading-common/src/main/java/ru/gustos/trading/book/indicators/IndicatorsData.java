package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.sql.SQLException;
import java.util.HashMap;

public class IndicatorsData {
    private HashMap<Integer,double[][]> data = new HashMap<>();
    private Sheet sheet;

    public IndicatorsData(Sheet sheet){
        this.sheet = sheet;
    }

    public void calc(Indicator ind) {
        calc(ind,0,sheet.size());
    }

    public void calc(Indicator ind, int from, int to){
        double[][] values =  new double[ind.getNumberOfLines()][sheet.size()];
        if (from!=0) {
            double[][] old = data.getOrDefault(ind.getId(), null);
            if (old != null)
                for (int i = 0;i<old.length;i++)
                    System.arraycopy(old[i], 0, values[i], 0, old[i].length);
        }
        ind.calcValues(sheet,values,from,to);
        data.put(ind.getId(),values);
    }

    public double get(Indicator indicator, int index) {
        return get(indicator.getId(),index);
    }
    public double getLine(Indicator indicator, int index, int line) {        return getLine(indicator.getId(),index,line);    }

    public double get(Indicator indicator, int index, int scale) {
        double[] data = get(indicator.getId());
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

    public int hasYesNo(Indicator indicator, int index, int scale) {
        int result = 0;
        double[] data = get(indicator.getId());
        if (scale==1) {
            double v = data[index];
            if (v== Indicator.YES)
                result=1;
            else if (v== Indicator.NO)
                result=2;
        } else {
            for (int i = 0;i<scale;i++) {
                if (index+i>=data.length) break;
                double v = data[index + i];
                if (v== Indicator.YES)
                    result|=1;
                else if (v== Indicator.NO)
                    result|=2;
            }
        }
        return result;
    }

    public double get(int id, int index) {
        return data.get(id)[0][index];
    }
    public double getLine(int id, int index, int line) {        return data.get(id)[line][index];    }

    public double[] get(int id){
        return data.get(id)[0];
    }
    public double[] getLine(int id, int line){
        return data.get(id)[line];
    }

    public void save(IndicatorsDb db) throws SQLException {
//        Integer[] keys = data.keySet().toArray(new Integer[0]);
//
//        String[] names = Arrays.stream(keys).map(id -> sheet.getLib().get(id).getName().toLowerCase()).toArray(String[]::new);
//        double[][] values =  Arrays.stream(keys).map(k->data.get(k)).toArray(double[][]::new);
//        db.save(sheet,sheet.getFrom(), names,values);
    }

    public void load(IndicatorsDb db) throws SQLException {
//        int[] keys = sheet.getLib().listIndicators().stream().mapToInt(Indicator::getId).toArray();
//        String[] names = Arrays.stream(keys).mapToObj(id -> sheet.getLib().get(id).getName().toLowerCase()).toArray(String[]::new);
//        db.load(sheet,names,keys,data);
    }
}
