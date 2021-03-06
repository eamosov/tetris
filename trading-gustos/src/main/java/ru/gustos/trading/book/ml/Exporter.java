package ru.gustos.trading.book.ml;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.indicators.*;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class Exporter {

    public static void string2file(String path, String s)  {
        try {
            try (Writer out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(path), "UTF-8"))){
                out.write(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static HashSet<Integer> ignored = null;
    private static HashSet<Integer> ignore(){
        if (ignored!=null) return ignored;
        HashSet<Integer> ignore = new HashSet<>();//Arrays.stream(new Integer[]{2, 3, EfreetSuccessIndicator.Id,PredictBuyIndicator.Id,PredictSellIndicator.Id,DemaTradeSuccIndicator.Id}).collect(Collectors.toList()));
        ignore.add(1);
        for (int i = 200;i<=1000;i++)
            ignore.add(i);
        return ignored = ignore;
    }

    public static ArrayList<Attribute> makeAttributes(Sheet sheet, int target){
        HashSet<Integer> ignore = ignore();
        ArrayList<Attribute> result = new ArrayList<>();

        for (Indicator ii : sheet.getLib().indicators) {
            if (!ignore.contains(ii.getId()))
                result.add(createAttribute(ii));
        }
        Indicator ii = sheet.getLib().get(target);
        result.add(createAttribute(ii));

        return result;
    }

    public static Instances makeBoolSet(String[] names, int instances){
        ArrayList<Attribute> attributes = new ArrayList<>();
        for (String name : names)
            attributes.add(new Attribute(name, Arrays.asList("false", "true")));
        Instances res = new Instances("data", attributes, instances);
        for (int i = 0;i<instances;i++)
            res.add(new DenseInstance(1,new double[names.length]));
        return res;

    }

    private static Attribute createAttribute(Indicator ii){
        if (ii.getResultType() == IndicatorResultType.NUMBER)
            return new Attribute(ii.getName());
        else
            return new Attribute(ii.getName(), Arrays.asList("false", "true"));

    }

    public static Instances initDataSet(Sheet sheet, int target){
        ArrayList<Attribute> infos = Exporter.makeAttributes(sheet, target);
        Instances data = new Instances("data", infos, 10);
        data.setClassIndex(infos.size()-1);
        return data;
    }

    public static Instances makeDataSet(Sheet sheet, int target, int from, int to){
        Instances data = initDataSet(sheet, target);
        for (int i = from;i<to;i++)
            addInstance(data,sheet,i,target);

        return data;
    }

    private static void addInstance(Instances data, Sheet sheet, int i, int target) {
        HashSet<Integer> ignore = ignore();
        double[] instance = new double[data.numAttributes()];
        int j = 0;
        for (Indicator ii : sheet.getLib().indicators) {
            if (!ignore.contains(ii.getId())){
                double v = sheet.getData().get(ii.getId(), i);
                if (ii.getResultType()== IndicatorResultType.YESNO && v<0) v = 0;
                if (ii.getResultType()== IndicatorResultType.YESNO && v>0 && v!=1) {
                    System.out.println("not 1 "+ii.getName()+" "+ii.getId()+" "+v);
                }
                instance[j] = v;
                j++;
            }
        }
        double v = sheet.getData().get(target, i);
        if (v<0) v = 0;
        instance[j] = v;
        Instance inst = new DenseInstance(1, instance);
        data.add(inst);
    }


    private static String lastName;
    public static int lastFrom;
    public static int lastTo;
    public static String doExport(Sheet sheet, int targetId, boolean train) {
        return doExport(sheet,targetId,train,false);
    }
    public static String doExport(Sheet sheet, int targetId, boolean train, boolean noZeroes){
        HashSet<Integer> ignore = ignore();

        int from,to;
//        if (train) {
//            from = sheet.getBarIndex(ZonedDateTime.of(2017, 12, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
//            to = sheet.getBarIndex(ZonedDateTime.of(2018, 3, 25, 0, 0, 0, 0, ZoneId.systemDefault()));
//        } else {
//            from = sheet.getBarIndex(ZonedDateTime.of(2018, 3, 25, 0, 0, 0, 0, ZoneId.systemDefault()));
//            to = sheet.getBarIndex(ZonedDateTime.of(2018, 4, 27, 0, 0, 0, 0, ZoneId.systemDefault()));
//        }
        if (train) {
            from = sheet.getBarIndex(ZonedDateTime.of(2018, 3, 15, 0, 0, 0, 0, ZoneId.systemDefault()));
            to = sheet.getBarIndex(ZonedDateTime.of(2018, 4, 28, 0, 0, 0, 0, ZoneId.systemDefault()));
        } else {
            from = sheet.getBarIndex(ZonedDateTime.of(2018, 4, 28, 0, 0, 0, 0, ZoneId.systemDefault()));
            to = sheet.getBarIndex(ZonedDateTime.of(2018, 5, 3, 0, 0, 0, 0, ZoneId.systemDefault()));
        }
        lastFrom = from;
        lastTo = to;
        String name = String.format("export_%s_%s", sheet.getLib().get(targetId).getName(), train ? "train" : "exam");
        lastName = name;

        StringBuilder sb = new StringBuilder();
        sb.append("@relation ").append(name).append("\n\n");

        for (Indicator ii : sheet.getLib().indicators)
            if (!ignore.contains(ii.getId()))
                addAttribute(sb,ii);

        addAttribute(sb,sheet.getLib().get(targetId));


        sb.append("\n");
        sb.append("@data\n");

        for (int i = from;i<to;i++) if (!noZeroes || sheet.getData().get(targetId,i)!=0){
            for (Indicator ii : sheet.getLib().indicators)
                if (!ignore.contains(ii.getId())) {
                    addValue(sb,sheet,i,ii);
                    sb.append(',');
                }
            addValue(sb,sheet,i, sheet.getLib().get(targetId));
            sb.append(",{").append(sheet.moments.get(i).weight).append("}");
            sb.append("\n");
        }
        return sb.toString();
//        string2file("d:/tetrislibs/export_efreet.csv",sb.toString());

    }

    private static void addValue(StringBuilder sb, Sheet sheet, int index, Indicator ii) {
        double v = sheet.getData().get(ii, index);
        if (ii.getResultType()== IndicatorResultType.YESNO)
            sb.append(v>0?"true":"false");
        else
            sb.append(String.format("%.4f", v).replace(',','.'));
    }

    public static void addAttribute(StringBuilder sb, Indicator ii) {
        sb.append("@attribute ").append(ii.getName());
        if (ii.getResultType()== IndicatorResultType.NUMBER)
            sb.append(" numeric");
        else
            sb.append(" {false,true}");
        sb.append("\n");

    }

    public static void shouldBuy() throws Exception{
        Sheet sheet = new Sheet(new IndicatorsLib("indicators_bot.json"));
        sheet.fromCache();
        SheetUtils.FillDecisions(sheet);
        sheet.calcIndicatorsNoPredict();
        sheet.getData().calc(sheet.getLib().get(250));
        String s = doExport(sheet, 250,true);
        string2file("d:/tetrislibs/"+lastName+".arff",s);
        s = doExport(sheet, 250,false);
        string2file("d:/tetrislibs/"+lastName+".arff",s);

    }

    public static void shouldSell() throws Exception{
        Sheet sheet = new Sheet(new IndicatorsLib("indicators_bot.json"));
        sheet.fromCache();
        SheetUtils.FillDecisions(sheet);
        sheet.calcIndicatorsNoPredict();
        sheet.getData().calc(sheet.getLib().get(251));
        String s = doExport(sheet, 251,true,true);
        string2file("d:/tetrislibs/"+lastName+".arff",s);
        s = doExport(sheet, 251,false,true);
        string2file("d:/tetrislibs/"+lastName+".arff",s);

    }

    public static void main(String[] args) throws Exception {
        shouldSell();
//        shouldBuy();
    }

    public static void export2tdf(String fname, Instances set) {
        StringBuilder sb = new StringBuilder();
        sb.append(set.attribute(set.classIndex()).name());
        for (int i = 0;i<set.numAttributes();i++)
            if (i!=set.classIndex())
                sb.append("\t").append(set.attribute(i).name());
        for (int j = 0;j<set.size();j++){
            sb.append("\n");
            Instance ii = set.get(j);
            sb.append(ii.value(ii.classIndex()));
            for (int i = 0;i<ii.numAttributes();i++)
                if (i!=ii.classIndex())
                    sb.append("\t").append(ii.value(i));

        }
        string2file(fname,sb.toString());

    }

    public static void export2arff(String fname, Instances set) {
        string2file(fname,set.toString());
    }
}
