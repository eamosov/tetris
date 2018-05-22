package ru.gustos.trading.book.ml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kotlin.Pair;
import org.apache.commons.io.FileUtils;
import ru.efreet.trading.bot.StatsCalculator;
import ru.efreet.trading.bot.TradeHistory;
import ru.efreet.trading.bot.TradesStats;
import ru.efreet.trading.bot.TradesStatsShort;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.TradeRecord;
import ru.efreet.trading.trainer.Metrica;
import ru.efreet.trading.utils.ZonedDateTimeType;
import ru.gustos.trading.GustosBotLogicParams;
import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.VecUtils;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

public class SimulationAnalyzer {


    public static String sourceFolder = "pops";
    public static String wekaOutFolder = "d:/tetrislibs/wekadata";
    public static String calcFolder = "popanal";
    public static int cpus = 4;
    public static class TM {
        public GustosBotLogicParams args;
        public TradesStatsShort result;
        public Metrica metrica;
    }

    static class PopResult {
        public double futureProfit;
        public double futureProfit2;
        public double futureProfit3;
        public int historySims;
        public double shake = -666;
        public double shake2 = -666;
        public TradeHistory history;
        public TM tm;
        public ZonedDateTime time;

        public TradesStats stats;
        public double worst;
        public double worst2;
        public double profitPerDay;


        PopResult(TM tm, ZonedDateTime time){
            this.tm = tm;
            this.time = time;
        }

        void freeHistory(){
            stats = new StatsCalculator().stats(history);
            worst = history.worstInterval(3);
            worst2 = history.worstInterval(6);
            profitPerDay = history.getProfitPerDay();
            history = null;
        }

        public void calcShake() {
            if (shake==-666) {
                shake = LogicUtils.shake(sheet, tm.args, 0.05, time.minusDays(10), time) / history.getProfitPerDay();
                shake2 = LogicUtils.shake(sheet, tm.args, 0.05, time.minusDays(30), time) / history.getProfitPerDay();
            }
        }
    }
    static class DayResult{
        public PopResult[] results;

        static DayResult fromJson(String json){
            return new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeType()).create().fromJson(json, DayResult.class);
        }

        String toJson(){
            return new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeType()).setPrettyPrinting().create().toJson(this);
        }
    }

    public static Sheet sheet;
    public static TM[][] populations;
    public static ZonedDateTime[] times;
    public static String[] fnames;
    public static DayResult[] result;
    static ForkJoinPool executor;


    private static void loadPopulations() throws IOException {
        String dir = sourceFolder;
        File[] files = new File(dir).listFiles();
        long[] tt = Arrays.stream(files).mapToLong(f -> Long.parseLong(f.getName().substring(0, 10))).toArray();
        Arrays.sort(tt);
//        tt = new long[]{tt[0]};
        populations = new TM[tt.length][];
        result = new DayResult[tt.length];
        times = new ZonedDateTime[tt.length];
        fnames = new String[tt.length];
        for (int i = 0;i<tt.length;i++){
            times[i] = ZonedDateTime.ofInstant(Instant.ofEpochSecond(tt[i]),ZoneId.systemDefault());
            fnames[i] = Long.toString(tt[i]);
            populations[i] = new Gson().fromJson(FileUtils.readFileToString(new File(dir+"/"+tt[i]+".population")),TM[].class);
        }
    }

    public static void loadResults(int n) throws IOException {
        String dir = "popanal2";//calcFolder;
//        String saveDir = "popanal2";
        File[] files = new File(dir).listFiles();
        long[] tt = Arrays.stream(files).mapToLong(f -> Long.parseLong(f.getName())).toArray();
        Arrays.sort(tt);
        n = Math.min(n,tt.length);
        result = new DayResult[n];
        times = new ZonedDateTime[n];
        fnames = new String[n];
        for (int i = 0;i<n;i++){
            times[i] = ZonedDateTime.ofInstant(Instant.ofEpochSecond(tt[i]),ZoneId.systemDefault());
            fnames[i] = Long.toString(tt[i]);
            result[i] = DayResult.fromJson(FileUtils.readFileToString(new File(dir+"/"+tt[i])));
//            for (PopResult r : result[i].results)
//                r.freeHistory();
//            System.gc();
//            Exporter.string2file(saveDir+"/"+tt[i],result[i].toJson());
            //FileUtils.readFileToString(new File(saveDir+"/"+tt[i])))
        }

    }

    static int nn;
    private static void doPopulation(int n) {
        TM[] tm = populations[n];
        ZonedDateTime time = times[n];
        Hashtable<ZonedDateTime,Integer> timeCounts = new Hashtable<>();
        DayResult r = new DayResult();
        result[n] = r;
        r.results = new PopResult[tm.length];
        ArrayList<CompletableFuture> futures = new ArrayList<>();
        if (executor==null)
            executor = new ForkJoinPool(cpus,ForkJoinPool.defaultForkJoinWorkerThreadFactory,null,true);
        nn = 0;
        for (int ii = 0;ii<tm.length;ii++){
//            CompletableFuture.supplyAsync(Supplier {
//                val steppedParams = copy(origin.args)
//                gene.step(steppedParams, it)
//                return@Supplier Triple(gene, it, TrainItem.of(steppedParams, function, metrica))
//            }, executor)
            final int i = ii;
            futures.add(CompletableFuture.supplyAsync((Supplier<Boolean>) () -> {
                TradeHistory history = LogicUtils.doLogic(sheet, tm[i].args, time.minusDays(60), time);
                for (TradeRecord tr : history.getTrades()) {
                    ZonedDateTime t = tr.getTime();
                    timeCounts.put(t, timeCounts.getOrDefault(t, 0) + 1);
                }
                r.results[i] = new PopResult(tm[i], time);
                r.results[i].history = history;
                history = LogicUtils.doLogic(sheet, tm[i].args, time, time.plusHours(96));
                r.results[i].futureProfit = history.profitBeforeExtended(time.plusHours(24));
                r.results[i].futureProfit2 = history.profitBefore(time.plusHours(24));
                r.results[i].futureProfit3 = history.profitBefore(time.plusHours(72));
                r.results[i].calcShake();
                nn++;
                if (nn%10==0)
                    System.out.println(nn*100/tm.length+"%");
                return true;
            }, executor));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        double prof = 0;
        int positive = 0;
        for (int i = 0;i<tm.length;i++){
            PopResult rr = r.results[i];
            if (rr.futureProfit>1)
                positive++;
            prof+=rr.futureProfit;
            for (TradeRecord tr : rr.history.getTrades()){
                ZonedDateTime t = tr.getTime();
                if (timeCounts.get(t)>1)
                    rr.historySims++;
            }
        }
        System.out.println(String.format("population %d: profitable %d, avg %.5g, time %s", n, positive, prof/tm.length,time.toString()));

        String path = calcFolder;
        new File(path).mkdir();
        Exporter.string2file(path+"/"+fnames[n],r.toJson());
    }

    private static void applyMinMax(Instances ii, double[] min, double[] max){
        for (int i = 0;i<ii.size();i++){
            Instance inst = ii.get(i);
            for (int j= 0;j<min.length;j++) if (ii.attribute(j).isNumeric()){
                double v = inst.value(j);
                v = (v-min[j])/(max[j]-min[j])*2-1;
                inst.setValue(j,v);
            }
        }
    }

    static double totalAvg = 1;
    static double totalSelected = 1;
    static double totalUsual = 1;
    static double totalTopUsual = 1;
    static double totalTop10 = 1;
    static double selectedTop10 = 1;
    static double money = 1000;
    static Instances popSet;
    static void resetStats(){
        totalAvg = 1;
        totalSelected = 1;
        totalUsual = 1;
        totalTopUsual = 1;
        totalTop10 = 1;
        selectedTop10 = 1;
        money = 1000;
    }

    private static void doPopulationSelection(int n, int history, int part, int fromBest) throws Exception {
        Instances set = initDataSet(true);
        popSet = initDataSet(false);
        for (int p = Math.max(0,n-1-history);p<n-1;p++){
            PopResult[] rr = result[p].results;
            for (int j = rr.length*part/100;j<rr.length;j++)
                if (ResultsToWeka.fieldsCheck(rr[j].tm.args)) {
                    addInstance(set, p, j, 1);//7.0/(n-p+7));
                }
        }
        if (set.size()==0) return;
        set.setClassIndex(set.numAttributes()-1);
        double[] dd = set.get(0).toDoubleArray();
        double[] min = dd.clone();
        double[] max = dd.clone();
        for (int i = 1;i<set.size();i++){
            dd = set.get(i).toDoubleArray();
            VecUtils.minMax(dd,min,max);
        }
        applyMinMax(popSet,min,max);
        Exporter.string2file(wekaOutFolder+"/pop"+n+".arff",popSet.toString());
        double[] profits = new double[set.size()];
        for (int i = 0;i<set.size();i++)
            profits[i] = set.get(i).value(set.numAttributes()-1);
        Arrays.sort(profits);
        double median = 1.0;
//        if (profits[profits.length/2]>1)
//            median = profits[profits.length/2];
//        for (int i = 0;i<set.size();i++) {
//            double v = set.get(i).value(set.numAttributes() - 1);
//            set.get(i).setValue(set.numAttributes()-1,v>median?1.0:0.0);
//        }


        applyMinMax(set,min,max);
//        SMO smo = new SMO();
//        smo.setKernel(new NormalizedPolyKernel());
        Classifier classifier = new LinearRegression();
        classifier.buildClassifier(set);
        Instances set2 = initDataSet(true);
        PopResult[] rr = result[n].results;
        int from = rr.length * part / 100;
        for (int j = from; j<rr.length; j++) {
            addInstance(set2, n, j,1);
        }
        applyMinMax(set2,min,max);
        set2.setClassIndex(set2.numAttributes()-1);
        Evaluation evaluation2 = new Evaluation(set);
        evaluation2.evaluateModel(classifier, set2);
//        double[][] cm = evaluation2.confusionMatrix();
//        System.out.println("win rate: " + cm[1][1]/cm[0][1]);
//        System.out.println("confusion: " + Arrays.deepToString(cm));
        double best = -1;
        double sum = 0;
        int profitable = 0;
        double[] bestprofits = new double[rr.length-from];
        ArrayList<Pair<Integer,Double>> sorted = new ArrayList<>();
        for (int j = from; j<rr.length; j++) {
            double[] distr2 = classifier.distributionForInstance(set2.instance(j-from));
            sorted.add(new Pair<>(j,distr2[0]));
            if (rr[j].futureProfit>1)
                profitable++;
            sum+=rr[j].futureProfit;
            best = Math.max(best,rr[j].futureProfit);
            bestprofits[j-from] = rr[j].futureProfit;
        }
        Arrays.sort(bestprofits);
        sorted.sort(Comparator.comparing(Pair<Integer, Double>::getSecond));
//        System.out.println(sorted.toString());
        int selectedIndex = sorted.get(sorted.size()-1-fromBest).getFirst();
        double top10Classifier = 0;
        double topUsual = 0;
        int cnt = Math.min(4,sorted.size());
        for (int i = 0;i<cnt;i++) {
            top10Classifier += rr[sorted.get(sorted.size() - 1 - i).getFirst()].futureProfit;
            topUsual+=rr[rr.length-1-i].futureProfit;
        }

        top10Classifier/=cnt;
        topUsual/=cnt;
        totalAvg *= sum/(rr.length-from);
        totalSelected *= rr[selectedIndex].futureProfit;
//        System.out.println("selectedIndex "+selectedIndex+", from array of "+sorted.size())
        double usual = rr[rr.length - 1].futureProfit;
        totalUsual *= usual;
        totalTopUsual *= topUsual;
//        totalTop10 *= bestprofits[bestprofits.length-30];
        selectedTop10*=top10Classifier;
        money*=rr[selectedIndex].futureProfit;
        System.out.println(String.format("pop %d: profit: %.5g, compare: %.5g, best: %.5g, profitable: %d, avg: %.5g, selectedTop10: %.5g, money %d", n,rr[selectedIndex].futureProfit, rr[selectedIndex].futureProfit/ usual,best,profitable,sum/(rr.length-from),top10Classifier, (int)money));

    }


    public static ArrayList<Attribute> makeAttributes(boolean numProfit){
        ArrayList<Attribute> res = new ArrayList<>();
        for (Method f :  ResultsToWeka.fields) {
            res.add(new Attribute(f.getName().substring(3)));
        }
//        res.add(new Attribute("metrica"));
//        res.add(new Attribute("pearson"));
        res.add(new Attribute("sma"));
        res.add(new Attribute("sma2"));
        res.add(new Attribute("relProfit"));
        res.add(new Attribute("relProfit2"));
        res.add(new Attribute("profit_perday"));
        res.add(new Attribute("profit_perday2"));
        res.add(new Attribute("trades"));
//        res.add(new Attribute("sims"));
//        res.add(new Attribute("shake"));
        res.add(new Attribute("shake2"));
        res.add(new Attribute("shake22"));
        res.add(new Attribute("shake23"));
        res.add(new Attribute("worst"));
        res.add(new Attribute("worst2"));
        if (numProfit)
            res.add(new Attribute("profit"));//, Arrays.asList("false", "true")));
        else
            res.add(new Attribute("profit", Arrays.asList("false", "true")));

//        res.add(new Attribute("metrica"));
        return res;
    }

    public static Instances initDataSet(boolean numProfit){
        ArrayList<Attribute> infos = makeAttributes(numProfit);
        Instances data = new Instances("data", infos, 10);
        return data;
    }

    private static DenseInstance addInstance(Instances set, int pop, int j, double weightk) throws InvocationTargetException, IllegalAccessException {
        PopResult[] rr = result[pop].results;
        double[] ii = new double[set.numAttributes()];
        TM tm = result[pop].results[j].tm;
        int i = ResultsToWeka.fieldsToData(ii, tm.args);
        TradesStats stats = result[pop].results[j].stats;
//        ii[i++] = Math.max(-1, tm.metrica.getValue());
//        ii[i++] = Math.max(-1, stats.getPearson());
        ii[i++] = Math.max(-1, stats.getSma10());
        ii[i++] = Math.max(-1, stats.getSma10()*stats.getSma10());
        ii[i++] = Math.max(-1, stats.getRelProfit());
        ii[i++] = Math.max(-1, stats.getRelProfit()*stats.getRelProfit());
        ii[i++] = Math.max(-1, rr[j].profitPerDay);
        ii[i++] = Math.max(-1, rr[j].profitPerDay*rr[j].profitPerDay);
        ii[i++] = Math.max(-1, stats.getTrades());
//        ii[i++] = rr[j].historySims;
//        ii[i++] = rr[j].shake;
        ii[i++] = rr[j].shake2;
        ii[i++] = rr[j].shake2*rr[j].shake2;
        ii[i++] = rr[j].shake2*rr[j].shake2*rr[j].shake2;
        ii[i++] = rr[j].worst;
        ii[i++] = rr[j].worst2;
        double profit = rr[j].futureProfit;

        boolean good = profit > 1;
        ii[i] = profit;//good ? 1.0 : 0;
        DenseInstance instance = new DenseInstance((good ? profit : 1 / profit)*weightk, ii);
        set.add(instance);
        ii = ii.clone();
        ii[i] = good? 1:0;
        instance = new DenseInstance(weightk, ii);
        popSet.add(instance);
        return instance;
    }


    private static void export() throws InvocationTargetException, IllegalAccessException {
        ResultsToWeka.initFields();
        Instances set = initDataSet(false);
        for (int pop = 0;pop<populations.length;pop++){
            PopResult[] rr = result[pop].results;
            for (int j = rr.length*9/10;j<rr.length;j++)
                addInstance(set,pop,j,1);

        }
        Exporter.string2file("d:/tetrislibs/wekadata/simpops3_shake5.arff",set.toString());
    }


    private static void parseParams(String[] args) {
        for (int i = 0;i<args.length;i+=2){
            switch (args[i]){
                case "--cpu":
                    cpus = Integer.parseInt(args[i+1]);
                case "--in":
                    sourceFolder = args[i+1];
                case "--out":
                    calcFolder = args[i+1];
            }
        }
    }

    public static void init() throws Exception {
        sheet = TestUtils.makeSheet("indicators_simple.json", Instrument.getBTC_USDT());
        ResultsToWeka.initFields();
    }

    public static void doSelection(int history, int part, int fromBest) throws Exception {
        resetStats();
        for (int i = 0;i<result.length;i++)
            doPopulationSelection(i, history,part, fromBest);

        System.out.println("total avg: "+totalAvg);
        System.out.println("total usual: "+totalUsual);
        System.out.println("total top10: "+totalTopUsual);
        System.out.println("total selected: "+totalSelected);
        System.out.println("total selected top 10: "+selectedTop10);
//        System.out.println("total selected logistic: "+totalSelectedLogistic);
//        System.out.println("total selected bayes: "+totalSelectedBayes);

    }

    public static void main(String[] args) throws Exception{
        parseParams(args);

        init();

        loadPopulations();

        for (int i = 0;i<populations.length;i++) {
            doPopulation(i);
//            doPopulationSelection(i);
        }
//        for (int i = 10;i<populations.length;i++)
//
//        export();
    }


}


