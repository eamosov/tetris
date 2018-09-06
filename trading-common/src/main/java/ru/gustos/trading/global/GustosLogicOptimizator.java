package ru.gustos.trading.global;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.indicators.GustosAverageRecurrent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.IntStream;

public class GustosLogicOptimizator {
    private double fee = 0.998;

    double[] prices;
    double[] minprices;
    double[] maxprices;
    double[] volumes;

    int from;
    public static final int INIT_CALC_FROM = 4000;

    int calcFrom = INIT_CALC_FROM;


    public GustosLogicOptimizator(InstrumentData data, int from, int to) {
        this.from = from;
        prices = data.bars.direct().stream().skip(from).limit(to - from).mapToDouble(XBar::getClosePrice).toArray();
        minprices = data.bars.direct().stream().skip(from).limit(to - from).mapToDouble(XBar::getMinPrice).toArray();
        maxprices = data.bars.direct().stream().skip(from).limit(to - from).mapToDouble(XBar::getMaxPrice).toArray();
        volumes = data.bars.direct().stream().skip(from).limit(to - from).mapToDouble(XBar::getVolume).toArray();
    }

    public void setCalcFrom(int calcFrom){
        this.calcFrom = calcFrom;
    }

    public double test() {
        return calc(new Params(), 1).profit;
    }

    public Pair<Params, Double> optimize(Params p) {
        Params result = null;
        double best = 0;
        for (int i = 0;i<5;i++){
            Pair<Params, Double> r = doOptimize(p,1);
//            System.out.println("it "+i+"="+r.getSecond());
            if (r.getSecond()>best){
                result = r.getFirst();
                best = r.getSecond();
            }
        }
        return new Pair<>(result,best);
    }

    public Pair<Params, Double> localOptimize(Params p) {
        Params result = null;
        double best = 0;
        for (int i = 0;i<5;i++){
            Pair<Params, Double> r = doOptimize(p,10);
//            System.out.println("it "+i+"="+r.getSecond());
            if (r.getSecond()>best){
                result = r.getFirst();
                best = r.getSecond();
            }
        }
        return new Pair<>(result,best);
    }

    private Pair<Params, Double> doOptimize(Params p, double k) {
        Params c = new Params(p);
        double best = estimate(c);
        for (int i = 0;i<10;i++){
            for (int j = 0;j<5;j++){
                Params cc = new Params(c);
                cc.randomize(1.0/(2*i*k+1));
                double v = estimate(cc);
                if (v>best){
                    best = v;
                    c = cc;
                }
            }
        }
        return new Pair<>(c,best);
    }

    public void makeCuts() {
        Params p = new Params();
        for (int i = 0; i < p.params.length; i++) {
            double[] d = cut1d(p, i);
            String s = Arrays.toString(d).replace(" ", "").replace("[", "").replace("]", "");
            System.out.println(s);
        }
    }

    public double[] cut1d(Params p, int param) {
        int from = Math.max(1, p.params[param] / 8);
        int to = p.params[param] * 4;
        int step = Math.max(1, (to - from) / 50);
        double[] v = new double[(to - from) / step + 1];
        Params pp = new Params(p);
        for (int i = from; i < to; i += step) {
            pp.params[param] = i;
            v[(i - from) / step] = estimate(pp);
        }
        return v;
    }


    private double estimate(Params p) {
        // можно собрать рандомные точки вокруг, а можно вообще поискать максимумы рядом.
        // в калке мб попробовать лимитировать дельту с одной операции, чтобы не привязывался слишком сильно к отдельным результатам
        int l = p.params.length;

        double sum = IntStream.range(0, l * 2).filter(i->!Params.fixed.contains(i)).parallel().mapToDouble(i -> {
            Params pp = new Params(p);
            pp.params[i%l] = i >= l ? Math.max(pp.params[i - l] + 1, pp.params[i - l] * 105 / 100) : Math.max(1,Math.min(pp.params[i] - 1, pp.params[i] * 95 / 100));
            return calc(pp, 0.03).estimate();
        }).sum();
        return sum;
    }

    public Stat calc(Params p, double limit) {
        return calc(p,limit,null);
    }

    public Stat calc(Params p, double limit, PLHistory history) {
        GustosAverageRecurrent buygar = new GustosAverageRecurrent(p.buyWindow(), p.buyVolumeWindow(), p.volumeShort(), p.volumePow1() * 0.1, p.volumePow2() * 0.1);
        GustosAverageRecurrent sellgar = new GustosAverageRecurrent(p.sellWindow(), p.sellVolumeWindow(), p.volumeShort(), p.volumePow1() * 0.1, p.volumePow2() * 0.1);
        double buyPrice = 0;
        buygar.feedNoReturn(prices[0], volumes[0]);
        sellgar.feedNoReturn(prices[0], volumes[0]);
        Stat result = new Stat();
        double max = 1;
        for (int i = 1; i < prices.length; i++) {
            buygar.feedNoReturn(prices[i], volumes[i]);
            sellgar.feedNoReturn(prices[i], volumes[i]);
            if (i>calcFrom) {
                if (buyPrice == 0) {
                    if (shouldBuy(i, buygar, p)) {
                        buyPrice = prices[i];
                        if (history!=null)
                            history.buyMoment(buyPrice,from+i);
                    }
                } else {
                    if (shouldSell(i, sellgar, p) || (history==null && i == prices.length - 1)) {
                        if (history!=null)
                            history.sellMoment(prices[i],from+i);
                        double profit = prices[i] / buyPrice * fee;
                        if (profit > 1 + limit) profit = 1 + limit;
                        if (profit < 1 - limit) profit = 1 - limit;
                        result.profitInTime *= (profit-1)*prices.length/(prices.length*3-i*2)+1;
                        result.profit *= profit;
                        if (result.profit > max)
                            max = result.profit;
                        result.dropdown = Math.min(result.dropdown, result.profit / max);
                        buyPrice = 0;
                        result.trades++;
                    }
                }
            }
        }
        return result;
    }

    private boolean shouldSell(int index, GustosAverageRecurrent r, Params p) {
        if (index == 0) return false;
        double sma = r.value();
        double sd = r.sd();
        return /*maxprices[index] >= sma + sd * p.sellDiv()*0.1 && */prices[index] > sma + sd * p.sellBoundDiv() * 0.1 && prices[index - 1] >= minprices[index];
    }

    private boolean shouldBuy(int index, GustosAverageRecurrent r, Params p) {
        if (index == 0) return false;
        double v = r.pvalue() - r.psd() * p.buyDiv() * 0.1;
        return minprices[index] <= v && maxprices[index] >= v && prices[index] < r.value() - r.sd() * p.buyBoundDiv() * 0.1 && prices[index - 1] < maxprices[index];
    }


    public static String[] paramNames = {"volumeShort", "buyWindow", "sellWindow", "buyVolumeWindow", "sellVolumeWindow", "buyDiv", "sellDiv", "buyBoundDiv", "sellBoundDiv", "volumePow1", "volumePow2"};

    public static class Params {
        public int[] params = new int[11];

        public int volumeShort() {
            return params[0];
        }

        public int buyWindow() {
            return params[1];
        }

        public int sellWindow() {
            return params[2];
        }

        public int buyVolumeWindow() {
            return params[3];
        }

        public int sellVolumeWindow() {
            return params[4];
        }

        public int buyDiv() {
            return params[5];
        }

        public int sellDiv() {
            return params[6];
        }

        public int buyBoundDiv() {
            return params[7];
        }

        public int sellBoundDiv() {
            return params[8];
        }

        public int volumePow1() {
            return params[9];
        }

        public int volumePow2() {
            return params[10];
        }

        public Params() {
            defaults();
        }

        public Params(Params p) {
            System.arraycopy(p.params, 0, params, 0, params.length);
        }

        Params defaults() {
            params[0] = 30;
            params[1] = 170;
            params[2] = 700;
            params[3] = 1700;
            params[4] = 1900;
            params[5] = 8;
            params[6] = 16;
            params[7] = 11;
            params[8] = 7;
            params[9] = 24;
            params[10] = 14;
//            params[0] = 30;
//            params[1] = 177;
//            params[2] = 702;
//            params[3] = 1842;
//            params[4] = 1891;
//            params[5] = 8;
//            params[6] = 16;
//            params[7] = 11;
//            params[8] = 7;
//            params[9] = 24;
//            params[10] = 14;
            return this;
        }

        static Params def = new Params();

        static HashSet<Integer> fixed = new HashSet<>(Arrays.asList(0, 3, 4, 6));

        public void randomize(double r) {
            for (int i = 0;i<params.length;i++) if (!fixed.contains(i)){
                double rnd = Math.random()*2-1;
                double ddif = def.params[i] * rnd * 0.9 * r;
                int dif = (int) ddif;
                if (dif==0 && Math.abs(rnd)>0.5)
                    dif = rnd>0?1:-1;
                params[i] = Math.max(1,params[i]+ dif);
            }
        }

        public Params dif(Params p) {
            for (int i = 0;i<params.length;i++)
                params[i]-=p.params[i];
            return this;
        }

        @Override
        public String toString() {
            return Arrays.toString(params);
        }
    }

    public static class Stat {
        public double profit = 1;
        public double profitInTime = 1;
        public int trades = 0;
        public double dropdown = 1;

        @Override
        public String toString() {
            return String.format("profit: %.3g, trades: %d, dropdown: %.3g", profit, trades, dropdown);
        }

        public double estimate() {
            return profitInTime * Math.sqrt(dropdown);
        }
    }
}
