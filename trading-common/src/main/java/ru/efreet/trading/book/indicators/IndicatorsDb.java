package ru.efreet.trading.book.indicators;

import ru.efreet.trading.book.Sheet;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Exchange;
import ru.efreet.trading.exchange.Instrument;

import java.sql.*;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IndicatorsDb {
    private Connection conn;

    public IndicatorsDb(String path) throws SQLException {
        Properties prop = new Properties();
        prop.setProperty("journal_mode", "WAL");
        conn = DriverManager.getConnection("jdbc:sqlite:"+path, prop);
    }

    public void updateTable(Sheet sheet) throws SQLException {
        String tableName = tableName(sheet);
        ResultSet r = conn.createStatement().executeQuery(String.format("PRAGMA table_info(%s)", tableName));
        if (r.isClosed()){
            conn.createStatement().execute(String.format("create table %s(time bigint primary key, %s)", tableName,makeColumns(sheet.getLib())));
        } else {
            HashSet<String> columns = new HashSet<String>();
            do {

                String name = r.getString(2);
                if (!name.equalsIgnoreCase("time"))
                    columns.add(name.toLowerCase());
            } while (r.next());
            for (IIndicator ind : sheet.getLib().listIndicators()) if (!columns.contains(ind.getName().toLowerCase()))
                conn.createStatement().execute(String.format("ALTER TABLE %s ADD COLUMN %s double", tableName,ind.getName().toLowerCase()));
        }
    }

    public void loadIndicators(Exchange exch, Instrument instrument, BarInterval interval, IndicatorsLib lib){

    }

    private String makeColumns(IndicatorsLib lib) {
        StringBuilder sb = new StringBuilder();
        for (IIndicator ind : lib.listIndicators()) {
            if (sb.length()>0)
                sb.append(", ");
            sb.append(ind.getName().toLowerCase()).append(" double");
        }
        return sb.toString();
    }

    private String tableName(Sheet sheet) {
        return tableName(sheet.exchange(),sheet.instrument(),sheet.interval());
    }
    private String tableName(Exchange exch, Instrument instrument, BarInterval interval){
        return exch.getName().toLowerCase()+"_"+instrument.toString().toLowerCase()+"_"+interval.toString().toLowerCase();
    }

    public void save(Sheet sheet, ZonedDateTime from, String[] names, double[][] data) throws SQLException {
        conn.setAutoCommit(false);
        String tableName = tableName(sheet);
        int cnt = data[0].length;
        String columns = Arrays.stream(names).collect(Collectors.joining(", "));
        Duration interval = sheet.interval().getDuration();
        for (int i = 0;i<cnt;i++) {
            int fi = i;
            String values = IntStream.range(0,data.length).mapToObj(n->d2s(data[n][fi])).collect(Collectors.joining(", "));
            String q = String.format("INSERT OR REPLACE INTO %s (time,%s) VALUES (%d,%s)", tableName, columns, from.plus(interval.multipliedBy(i)).toEpochSecond(), values);
//            System.out.println(q);
            conn.createStatement().execute(q);
        }
        conn.commit();
        conn.setAutoCommit(true);
    }

    private String d2s(double d){
        if (Double.isNaN(d))
            d = -6666666666.3;
        return Double.toString(d);
    }

    private double d2d(double d){
        if (d==-6666666666.3)
            d = Double.NaN;
        return d;
    }

    public void load(Sheet sheet, String names[], int[] ids, HashMap<Integer, double[]> data) throws SQLException {
        String columns = Arrays.stream(names).collect(Collectors.joining(", "));
        long from = sheet.getFrom().toEpochSecond();
        int size = conn.createStatement().executeQuery(String.format("SELECT COUNT(*) FROM %s where time>=%d", tableName(sheet),from)).getInt(1);
        double[][] res = new double[ids.length][size+1];


        String q = String.format("SELECT time,%s FROM %s where time>=%d", columns, tableName(sheet), from);
        System.out.println(q);
        ResultSet r = conn.createStatement().executeQuery(q);
        if (r.isClosed()) return;
        int jj = 0;
        do {
            if (jj==0){
                long time = r.getLong(1);
                if (time>from){
                    System.out.println(String.format("inconsistent from %d, %d",time,from));
                    return;
                }
            }
            for (int i = 0;i<ids.length;i++)
                res[i][jj] = d2d(r.getDouble(i+2));

            jj++;
        } while (r.next());
        for (int i = 0;i<ids.length;i++)
            data.put(ids[i],res[i]);
    }

    public void clear(Sheet sheet) throws SQLException {
        conn.createStatement().execute("drop table "+tableName(sheet));
        updateTable(sheet);
    }
}
