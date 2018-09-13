package ru.gustos.trading.global;

import com.google.common.collect.Sets;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.MarketBar;
import ru.efreet.trading.bars.XBar;
import weka.core.Instances;

import java.util.*;


public class DecisionManager {
    public static final int calcAllFrom = 60 * 24 * 7 * 2;
    public int calcModelFrom = 60 * 24 * 7 * 3;

    public static final boolean LOGS = true;

    CalcConfig config;

    public int dontRenewAfter = Integer.MAX_VALUE;

    public HashSet<String> ignoreBuy = Sets.newHashSet();//"gustosBuy", "gustosSell");//,"sd_lag1","sd_delta1","sd_lag2","sd_delta2","macd0","macd1","macd2","macd3");
    public HashSet<String> ignoreSell = Sets.newHashSet();//"gustosBuy", "gustosSell");//,"d2vol","mm2vol","d2vol_n","mm2vol_n");
    HashSet<String> ignore(boolean buy) {
        return buy ? ignoreBuy : ignoreSell;
    }


    public InstrumentData data;

    public InstrumentData futuredata; // for pizdunstvo check

    public PLHistory plhistoryBase;
    public PLHistory plhistoryClassifiedBuy;
    public PLHistory plhistoryClassifiedSelected;

    public DecisionCalc calc;
    public DecisionModels models;

    public ArrayList<Pair<Instances,Instances>> export;

    int cpus;


    public DecisionManager(CalcConfig config, InstrumentData data, int cpus, boolean onlyCalc, int modelFrom) {
        this.config = config == null ? new CalcConfig() : config;
        this.data = data;
        calc = new DecisionCalc(this, onlyCalc);
        GustosBuySellMomentCalculator buysell = new GustosBuySellMomentCalculator(calc);
//        calc.setBuySellCalcs(data.buydata!=null?new QlikBuyMomentCalculator(this):buysell, buysell);
        calc.setBuySellCalcs(buysell, buysell);

        if (modelFrom != 0)
            calcModelFrom = modelFrom;
        this.cpus = cpus;
        plhistoryBase = new PLHistory(data.instrument.toString(), data.global != null ? data.global.planalyzer1 : null);
        plhistoryClassifiedBuy = new PLHistory(data.instrument.toString(), data.global != null ? data.global.planalyzer2 : null);
        plhistoryClassifiedSelected = new PLHistory(data.instrument.toString(), data.global != null ? data.global.planalyzer3 : null);
        models = new DecisionModels(this);
        calc.calcTillEnd();
    }

    public void makeExport(){
         export = new ArrayList<>();
    }

    public void checkNeedRenew(boolean thread){
        models.checkNeedRenew(thread);
    }


    public void addBar(XBar bar, MarketBar marketBar) {
        models.checkTakeNewModel();
        data.addBar(bar, marketBar);
        calc.calcTillEnd();

    }

    double limit() {
        double limit = config.momentLimit;
        if (limit == 0) {
            if (data.instrument.component1().equalsIgnoreCase("NEO") || data.instrument.component1().equalsIgnoreCase("XLM"))
                limit = 0.04;
            else if (data.instrument.component1().equalsIgnoreCase("BNB"))
                limit = 0.05;
            else if (data.instrument.component1().equalsIgnoreCase("LTC") || data.instrument.component1().equalsIgnoreCase("ETH"))
                limit = 0.025;
            else
                limit = 0.03;
        }
        return limit;
    }



    @NotNull
    public Decision decision() {
//        System.out.println(calc.calcIndex+" "+calcModelFrom+" "+calcAllFrom+" "+data.instrument);
        MomentData mldata = data.data.get(calc.calcIndex - 1);
        if (!hasModel()) return Decision.NONE;
        boolean classifiedBuy = data.helper.get(mldata, "@goodBuy|main") > 0.5;
        boolean classifiedSell = true;//data.helper.get(mldata, "@goodSell|main") > 0.5;


        boolean gbuy = data.buys.get(calc.calcIndex - 1);//data.helper.get(mldata, "gustosBuy") > 0.5;
        boolean gsell = data.sells.get(calc.calcIndex - 1);//data.helper.get(mldata, "gustosSell") > 0.5;
        if (classifiedBuy && gbuy) return Decision.BUY;
        if (classifiedSell && gsell) return Decision.SELL;
        return Decision.NONE;
    }


    public boolean hasModel() {
        return models.hasModel();
    }

    int calcIndex(){
        return calc.calcIndex;
    }
}


