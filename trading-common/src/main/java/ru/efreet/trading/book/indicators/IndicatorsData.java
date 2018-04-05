package ru.efreet.trading.book.indicators;

import ru.efreet.trading.book.Sheet;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;

public class IndicatorsData {
    private HashMap<Integer,double[]> data = new HashMap<>();
    private Sheet sheet;

    public IndicatorsData(Sheet sheet){
        this.sheet = sheet;
    }

    public void calc(IIndicator ind){
        double[] values =  new double[sheet.moments.size()];
        ind.calcValues(sheet,values);
        data.put(ind.getId(),values);
    }

    public double get(IIndicator indicator, int index) {
        return data.get(indicator.getId())[index];
    }

    public double get(int id, int index) {
        return data.get(id)[index];
    }

    public void save(IndicatorsDb db) throws SQLException {
        Integer[] keys = data.keySet().toArray(new Integer[0]);

        String[] names = Arrays.stream(keys).map(id -> sheet.getLib().get(id).getName().toLowerCase()).toArray(String[]::new);
        double[][] values =  Arrays.stream(keys).map(k->data.get(k)).toArray(double[][]::new);
        db.save(sheet,sheet.getFrom(), names,values);
    }

    public void load(IndicatorsDb db) throws SQLException {
        int[] keys = Arrays.stream(sheet.getLib().listIndicators()).mapToInt(IIndicator::getId).toArray();
        String[] names = Arrays.stream(keys).mapToObj(id -> sheet.getLib().get(id).getName().toLowerCase()).toArray(String[]::new);
        db.load(sheet,names,keys,data);
    }
}
