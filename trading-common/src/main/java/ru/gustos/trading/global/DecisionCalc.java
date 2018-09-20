package ru.gustos.trading.global;

import kotlin.Pair;
import ru.efreet.trading.bars.MarketBar;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.RecurrentValues;
import ru.gustos.trading.book.SheetUtils;
import weka.core.Instance;
import weka.core.Instances;

import java.time.Duration;
import java.util.ArrayList;

import static ru.gustos.trading.global.DecisionManager.calcAllFrom;

public class DecisionCalc {
    int calcIndex = 0;
    int targetCalcedTo = 0;

    boolean gbuy, gsell;
    double price, minprice, maxprice;
    long time;
    RecurrentValues values;
    PLHistory gustosProfit;

    InstrumentData data;
    DecisionManager manager;

    BuyMomentCalculator buy;
    SellMomentCalculator sell;

    boolean onlyCalc;

    public boolean oldInstr() {
//        return false;
        String s = data.instrument.component1();
        return s.equals("BTC") || s.equals("LTC") || s.equals("NEO") || s.equals("XRP");
    }


    public DecisionCalc(DecisionManager manager, boolean onlyCalc) {
        this.manager = manager;
        this.data = manager.data;
        this.onlyCalc = onlyCalc;
        gustosProfit = new PLHistory(null, null);
//        volumes = new Volumes(data, false, false);
        values = new RecurrentValues(data);
    }

    public void setBuySellCalcs(BuyMomentCalculator buy, SellMomentCalculator sell) {
        this.buy = buy;
        this.sell = sell;
    }

    public MomentDataHelper helper() {
        return data.helper;
    }

    public MomentDataHelper resulthelper() {
        return data.resulthelper;
    }

    public MomentDataHelper buyhelper() {
        return data.buyhelper;
    }

    public MomentDataHelper sellhelper() {
        return data.sellhelper;
    }

    public void calcTillEnd() {
        while (calcIndex < data.size())
            doNext();
    }

    private void doNext() {
//        volumes.calc(calcIndex);
        values.feed(calcIndex);
        if (data.buydata != null)
            calcUsualDetector(calcIndex);
        calcUsual(calcIndex);
        if (calcIndex >= calcAllFrom && !onlyCalc) {
            calcTargets(calcIndex);
            calcPredictions(calcIndex);
            if (data.buydata != null)
                calcTargetsDetector(calcIndex);
//            if (DETECTOR) {
//            calcTargetsDetector(calcIndex);
//            calcPredictionsDetector(calcIndex);
//            }
        }
        calcIndex++;
    }

    public void calcAllDetectorTargets() {
        for (int i = calcAllFrom; i < calcIndex; i++)
            calcTargetsDetector(i);
    }


    private void calcUsualDetector(int index) {
        XBar bar = data.bars.get(index);
        MomentData mldata = data.buydata.get(index);
        buyhelper().put(mldata, "d2vol", values.deltaToVolumeShort.value() / values.deltaToVolume.value());
        buyhelper().put(mldata, "d2voln", values.deltaToVolumeShort.value());
        buyhelper().put(mldata, "mm2vol", values.maxminToVolumeShort.value() / values.maxminToVolume.value());
        buyhelper().put(mldata, "mm2voln", values.maxminToVolumeShort.value());

        buyhelper().put(mldata, "d2mm", values.deltaToMmShort.value() / values.deltaToMm.value());
        buyhelper().put(mldata, "d2mmn", values.deltaToMmShort.value());

        buyhelper().put(mldata, "mm", values.maxminShort.value() / values.maxmin.value());
        buyhelper().put(mldata, "change0", values.change0.value());
        buyhelper().put(mldata, "change1", values.change1.value());

        buyhelper().put(mldata, "upCandles1", SheetUtils.upCandles(data, index, 1));
        buyhelper().put(mldata, "upCandles2", SheetUtils.upCandles(data, index, 5));
        buyhelper().put(mldata, "downCandles1", SheetUtils.downCandles(data, index, 1));
        buyhelper().put(mldata, "downCandles2", SheetUtils.downCandles(data, index, 5));
//
        buyhelper().put(mldata, "toAvgSd", div(price - values.gustosAvg.value(), values.gustosAvg.sd()));
        buyhelper().put(mldata, "toAvgSdMax", div(maxprice - values.gustosAvg.value(), values.gustosAvg.sd()));
        buyhelper().put(mldata, "toAvgSdMin", div(minprice - values.gustosAvg.value(), values.gustosAvg.sd()));
        buyhelper().put(mldata, "sd", values.gustosAvg.sd() / price * 10);
        buyhelper().put(mldata, "toAvgSdMaxP", div(maxprice - values.gustosAvg.pvalue(), values.gustosAvg.psd()));
        buyhelper().put(mldata, "toAvgSdMinP", div(minprice - values.gustosAvg.pvalue(), values.gustosAvg.psd()));

        buyhelper().put(mldata, "toAvgSd2", div(price - values.gustosAvg2.value(), values.gustosAvg2.sd()));
        buyhelper().put(mldata, "toAvgSdMax2", div(maxprice - values.gustosAvg2.value(), values.gustosAvg2.sd()));
        buyhelper().put(mldata, "toAvgSdMin2", div(minprice - values.gustosAvg2.value(), values.gustosAvg2.sd()));
        buyhelper().put(mldata, "sd2", values.gustosAvg2.sd() / price * 10);
        buyhelper().put(mldata, "toAvgSdMaxP2", div(maxprice - values.gustosAvg2.pvalue(), values.gustosAvg2.psd()));
        buyhelper().put(mldata, "toAvgSdMinP2", div(minprice - values.gustosAvg2.pvalue(), values.gustosAvg2.psd()));
        buyhelper().put(mldata, "toAvgSdMaxPP2", div(maxprice - values.gustosAvg2.pvalue2(), values.gustosAvg2.psd2()));
        buyhelper().put(mldata, "toAvgSdMinPP2", div(minprice - values.gustosAvg2.pvalue2(), values.gustosAvg2.psd2()));
//
//        buyhelper().put(mldata, "toAvgSd3", div(price - values.gustosAvg3.value(), values.gustosAvg3.sd()));
//        buyhelper().put(mldata, "toAvgSdMax3", div(maxprice - values.gustosAvg3.value(), values.gustosAvg3.sd()));
//        buyhelper().put(mldata, "toAvgSdMin3", div(minprice - values.gustosAvg3.value(), values.gustosAvg3.sd()));
//        buyhelper().put(mldata, "sd3", values.gustosAvg3.sd() / price * 10);
//        buyhelper().put(mldata, "toAvgSdMaxP3", div(maxprice - values.gustosAvg3.pvalue(), values.gustosAvg3.psd()));
//        buyhelper().put(mldata, "toAvgSdMinP3", div(minprice - values.gustosAvg3.pvalue(), values.gustosAvg3.psd()));

        buyhelper().put(mldata, "macd0", values.macd0.value());
        buyhelper().put(mldata, "macd1", values.macd1.value());
        buyhelper().put(mldata, "stoh0", values.stoh0.percent());
        buyhelper().put(mldata, "stoh1", values.stoh1.percent());
        buyhelper().put(mldata, "stoh2", values.stoh2.percent());
        buyhelper().put(mldata, "rsi0", values.rsi0.value());
        buyhelper().put(mldata, "rsi1", values.rsi1.value());
        buyhelper().put(mldata, "rsi2", values.rsi2.value());

        helper().put(mldata, "vdema0", values.vdema0.value());
        helper().put(mldata, "vdema1", values.vdema1.value());
        helper().put(mldata, "vdema2", values.vdema2.value());
        helper().put(mldata, "vdema3", values.vdema3.value());
        helper().put(mldata, "vdema4", values.vdema4.value());

        helper().put(mldata, "change0", values.change0.value());
        helper().put(mldata, "change1", values.change1.value());
        helper().put(mldata, "change2", values.change2.value());
        helper().put(mldata, "change3", values.change3.value());
        helper().put(mldata, "change4", values.change4.value());

        buyhelper().put(mldata, "volumeBurst", values.volumeShort.value() / values.volumeLong.value());
        buyhelper().put(mldata, "volumeBurst2", values.volumeShort2.value() / values.volumeLong2.value());
//        buyhelper().put(mldata, "volume", m.bar.getVolume());
//        buyhelper().put(mldata, "price", m.bar.getClosePrice());

        int[] lags = new int[]{1, 2, 3, 6, 10, 20};
        for (int i = 0; i < lags.length; i++)
            if (lags[i] < index)
                buyhelper().putLagged(mldata, data.buydata.get(index - lags[i]), lags[i]);
    }

    private void calcUsual(int index) {
        XBar bar = data.bars.get(index);
        MomentData mldata = data.data.get(index);
        MomentData prevmldata = index > 0 ? data.data.get(index - 1) : mldata;
        MomentData prevmldata2 = index > 1 ? data.data.get(index - 2) : mldata;
        price = bar.getClosePrice();
        maxprice = bar.getMaxPrice();
        minprice = bar.getMinPrice();
        time = bar.getEndTime().toEpochSecond();

        gbuy = buy.shouldBuy();
        gsell = sell.shouldSell();
        data.buys.set(calcIndex, gbuy);
        data.sells.set(calcIndex, gsell);
        if (gsell || price > values.gustosAvg.value()/* || price>values.gustosAvg4.value()*/)
            mldata.ignore = true;

//        gbuy = CalcUtils.gustosBuy(data, index, values.gustosAvg, values.gustosParams);
//        gsell = CalcUtils.gustosSell(data, index, values.gustosAvg4, values.gustosParams);
//        gbuy = CalcUtils.gustosBuy(data, index, values.gustosAvgBuy, values.gustosParams);
//        gsell = CalcUtils.gustosSell(data, index, values.gustosAvgSell, values.gustosParams);

//        helper().put(mldata, "gustosBuy", gbuy ? 1.0 : 0, true);
//        helper().put(mldata, "gustosSell", gsell ? 1.0 : 0, true);


        helper().put(mldata, "toAvgSd", div(price - values.gustosAvg.value(), values.gustosAvg.sd()));
        helper().put(mldata, "toAvgSdMax", div(maxprice - values.gustosAvg.value(), values.gustosAvg.sd()));
        helper().put(mldata, "toAvgSdMin", div(minprice - values.gustosAvg.value(), values.gustosAvg.sd()));
        helper().put(mldata, "sd", values.gustosAvg.sd() / price * 10);

        helper().put(mldata, "toAvgSd2", div(price - values.gustosAvg2.value(), values.gustosAvg2.sd()));
        helper().put(mldata, "toAvgSdMax2", div(maxprice - values.gustosAvg2.value(), values.gustosAvg2.sd()));
        helper().put(mldata, "toAvgSdMin2", div(minprice - values.gustosAvg2.value(), values.gustosAvg2.sd()));
        helper().put(mldata, "sd2", values.gustosAvg2.sd() / price * 10);

        helper().put(mldata, "toAvgSd3", div(price - values.gustosAvg3.value(), values.gustosAvg3.sd()));
        helper().put(mldata, "toAvgSdMax3", div(maxprice - values.gustosAvg3.value(), values.gustosAvg3.sd()));
        helper().put(mldata, "toAvgSdMin3", div(minprice - values.gustosAvg3.value(), values.gustosAvg3.sd()));
        helper().put(mldata, "sd3", values.gustosAvg3.sd() / price * 10);

        helper().put(mldata, "toAvgSd4", div(price - values.gustosAvg4.value(), values.gustosAvg4.sd()));
        helper().put(mldata, "toAvgSdMax4", div(maxprice - values.gustosAvg4.value(), values.gustosAvg4.sd()));
        helper().put(mldata, "toAvgSdMin4", div(minprice - values.gustosAvg4.value(), values.gustosAvg4.sd()));
        helper().put(mldata, "sd4", values.gustosAvg4.sd() / price * 10);

        helper().putLagged(mldata, "toAvgSd", prevmldata, 1);
        helper().putLagged(mldata, "toAvgSd2", prevmldata, 1);
        helper().putLagged(mldata, "toAvgSdMin", prevmldata, 1);
        helper().putLagged(mldata, "toAvgSdMax", prevmldata, 1);
        helper().putLagged(mldata, "toAvgSdMin2", prevmldata, 1);
        helper().putLagged(mldata, "toAvgSdMax2", prevmldata, 1);
        helper().putLagged(mldata, "sd", prevmldata, 1);
//        helper().putLagged(mldata, "sd2", prevmldata, 1);
        helper().putLagged(mldata, "sd4", prevmldata, 1);

//        helper().putLagged(mldata,"toAvgSd",prevmldata2,2);
//        helper().putLagged(mldata,"toAvgSd2",prevmldata2,2);
//        helper().putLagged(mldata,"toAvgSdMin",prevmldata2,2);
//        helper().putLagged(mldata,"toAvgSdMax",prevmldata2,2);
//        helper().putLagged(mldata,"toAvgSdMin2",prevmldata2,2);
//        helper().putLagged(mldata,"toAvgSdMax2",prevmldata2,2);
//        helper().putLagged(mldata,"sd",prevmldata2,2);
//        helper().putLagged(mldata,"sd2",prevmldata2,2);

//        helper().put(mldata, "toAvgSdB", div(price - values.gustosAvgBuy.value(), values.gustosAvgBuy.sd()));
//        helper().put(mldata, "toAvgSdS", div(price - values.gustosAvgSell.value(), values.gustosAvgSell.sd()));
//        helper().put(mldata, "sdB", values.gustosAvgBuy.sd() / price * 10);
//        helper().put(mldata, "sdS", values.gustosAvgSell.sd() / price * 10);

        helper().put(mldata, "d2vol", values.deltaToVolumeShort.value() / values.deltaToVolume.value());
        helper().put(mldata, "mm2vol", values.maxminToVolumeShort.value() / values.maxminToVolume.value());
        helper().put(mldata, "d2vol_n", values.deltaToVolumeShort.value());
        helper().put(mldata, "mm2vol_n", values.maxminToVolumeShort.value());

//        helper().put(mldata, "d2mm",values.deltaToMmShort.value()/values.deltaToMm.value());
//        helper().put(mldata, "d2mm_n",values.deltaToMmShort.value());

        helper().put(mldata, "mm", values.maxminShort.value() / values.maxmin.value());


        helper().put(mldata, "macd0", values.macd0.value());
        helper().put(mldata, "macd1", values.macd1.value());
        helper().put(mldata, "macd2", values.macd2.value());
        helper().put(mldata, "macd3", values.macd3.value());
//        helper().put(mldata, "macd4", values.macd4.value());
//        helper().put(mldata, "pmacd0", values.macd0.pvalue());
//        helper().put(mldata, "pmacd1", values.macd1.pvalue());
//        helper().put(mldata, "pmacd2", values.macd2.pvalue());
//        helper().put(mldata, "pmacd3", values.macd3.pvalue());
//        helper().put(mldata, "pmacd4", values.macd4.pvalue());
        helper().putLagged(mldata, "macd0", prevmldata, 1);
        helper().putLagged(mldata, "macd1", prevmldata, 1);
        helper().putLagged(mldata, "macd2", prevmldata, 1);
        helper().putLagged(mldata, "macd3", prevmldata, 1);
//        helper().putLagged(mldata, "macd4", prevmldata, 1);

        helper().put(mldata, "vdema0", values.vdema0.value());
        helper().put(mldata, "vdema1", values.vdema1.value());
        helper().put(mldata, "vdema2", values.vdema2.value());
        helper().put(mldata, "vdema3", values.vdema3.value());
//        helper().put(mldata, "vdema4", values.vdema4.value());
//        helper().put(mldata, "vdema5", values.vdema5.value());
//        helper().put(mldata, "vdema6", values.vdema6.value());
//        helper().put(mldata, "vdema7", values.vdema7.value());
//        helper().putLagged(mldata,"vdema0",prevmldata,1);
//        helper().putLagged(mldata,"vdema1",prevmldata,1);
//        helper().putLagged(mldata,"vdema2",prevmldata,1);
//        helper().putLagged(mldata,"vdema3",prevmldata,1);
//        helper().putLagged(mldata,"vdema4",prevmldata,1);
//        helper().put(mldata, "pvdema0", values.vdema0.pvalue());
//        helper().put(mldata, "pvdema1", values.vdema1.pvalue());
//        helper().put(mldata, "pvdema2", values.vdema2.pvalue());
//        helper().put(mldata, "pvdema3", values.vdema3.pvalue());
//        helper().put(mldata, "pvdema4", values.vdema4.pvalue());
//        helper().put(mldata, "vmacd0", values.vmacd0.value());
//        helper().put(mldata, "vmacd1", values.vmacd1.value());
//        helper().put(mldata, "vmacd2", values.vmacd2.value());

        //        helper().put(mldata, "pmacd0", values.macd0.value() - values.macd0.pvalue());
//        helper().put(mldata, "pmacd1", values.macd1.value() - values.macd1.pvalue());
//        helper().put(mldata, "pmacd2", values.macd2.value() - values.macd2.pvalue());
//        helper().put(mldata, "rsi0", values.rsi0.value());
        helper().put(mldata, "rsi1", values.rsi1.value());
        helper().put(mldata, "rsi2", values.rsi2.value());
        helper().put(mldata, "rsiv0", values.rsiv0.value());
        helper().put(mldata, "rsiv1", values.rsiv1.value());
        helper().put(mldata, "rsiv2", values.rsiv2.value());
//        helper().put(mldata, "stoh0", values.stoh0.percent());
        helper().put(mldata, "stoh1", values.stoh1.percent());
        helper().put(mldata, "stoh2", values.stoh2.percent());
        helper().put(mldata, "stoh3", values.stoh3.percent());
        helper().put(mldata, "stoh4", values.stoh4.percent());
//        helper().put(mldata, "change0", values.change0.value());
        helper().put(mldata, "change1", values.change1.value());
        helper().put(mldata, "change2", values.change2.value());
        helper().put(mldata, "change3", values.change3.value());
        helper().put(mldata, "change4", values.change4.value());

//        helper().put(mldata, "vdema0_delta1", values.vdema0.pvalue() - values.vdema0.value());
        helper().put(mldata, "vdema1_delta1", values.vdema1.pvalue() - values.vdema1.value());

        if (!oldInstr()) {
//        for (int i = 1;i<10;i++)
//            helper().put(mldata, "vdema0_delta"+i, values.vdema0.history(i) - values.vdema0.value());
            for (int i = 1; i < 10; i++)
                helper().put(mldata, "vdema0_lag" + i, values.vdema0.history(i));
//            for (int i = 1; i < 10; i++)
//                helper().put(mldata, "vdema1_lag" + i, values.vdema1.history(i));
//            for (int i = 0; i < 10; i++)
//                helper().put(mldata, "vdema1_4h_lag" + i, values.vdema1_4h.history(i));
//            for (int i = 0; i < 1; i++)
//                helper().put(mldata, "vdema0_4h_lag" + i, values.vdema0_4h.history(i));
//            helper().put(mldata, "vdema0_lag10", values.vdema0.history(10));
//            helper().put(mldata, "vdema0_lag20", values.vdema0.history(20));
        }


//        if (calcIndex>20) {
//            for (int i = 1; i < 5; i++)
//                helper().put(mldata, "price_delta" + i, price - data.bar(index-i).getClosePrice());
//            helper().put(mldata, "price_delta10", price - data.bar(index-10).getClosePrice());
//            helper().put(mldata, "price_delta20", price - data.bar(index-20).getClosePrice());
//        }

        helper().putDelta(mldata, "macd1", prevmldata, 1);
//        helper().putDelta(mldata, "macd0", prevmldata, 1);
//        helper().putDelta(mldata, "rsi0", prevmldata, 1);
//        helper().putDelta(mldata, "rsi1", prevmldata, 1);
//        helper().putDelta(mldata, "stoh0", prevmldata, 1);
//        helper().putDelta(mldata, "stoh1", prevmldata, 1);
//        helper().putDelta(mldata, "vdema0", prevmldata, 2);
//        helper().putDelta(mldata, "vdema1", prevmldata, 2);
//        helper().putDelta(mldata, "macd0", prevmldata, 2);
//        helper().putDelta(mldata, "macd1", prevmldata, 2);
//        helper().putDelta(mldata, "rsi0", prevmldata, 2);
//        helper().putDelta(mldata, "rsi1", prevmldata, 2);
//        helper().putDelta(mldata, "stoh0", prevmldata, 2);
//        helper().putDelta(mldata, "stoh1", prevmldata, 2);
        //            helper().put(mldata,"rising",sheet.bar(index-1).getClosePrice() < sheet.bar(index).getMinPrice()?1:0);
//            helper().put(mldata,"falling",sheet.bar(index-1).getClosePrice() >= sheet.bar(index).getMaxPrice()?1:0);

//        helper().put(mldata, "toAvg0", ((price - values.ema0.value()) / price));
//        helper().put(mldata, "toAvg1", ((price - values.ema1.value()) / price));
//        helper().put(mldata, "toAvg2", ((price - values.ema2.value()) / price));
//        helper().put(mldata, "toAvg3", ((price - values.ema3.value()) / price));
//        helper().put(mldata, "toAvgSdU0", (div(price - values.ema0.value(), values.sd0.value())));
//        helper().put(mldata, "toAvgSdU1", (div(price - values.ema1.value(), values.sd1.value())));
//        helper().put(mldata, "toAvgSdU2", (div(price - values.ema2.value(), values.sd2.value())));
//        helper().put(mldata, "toAvgSdU3", (div(price - values.ema3.value(), values.sd3.value())));
//        helper().put(mldata, "sd_usual0", Math.sqrt(values.sd0.value()) / price * 10);
//        helper().put(mldata, "sd_usual1", Math.sqrt(values.sd1.value()) / price * 10);
//        helper().put(mldata, "sd_usual2", Math.sqrt(values.sd2.value()) / price * 10);
//        helper().put(mldata, "sd_usual3", Math.sqrt(values.sd3.value()) / price * 10);
//
//        helper().put(mldata, "volumeBurst", values.volumeShort.value() / values.volumeLong.value());
//        helper().put(mldata, "volumeBurst2", values.volumeShort2.value() / values.volumeLong2.value());
//            helper().put(mldata, "volumeBurstBool", values.volumeShort.value() / values.volumeLong.value() > 3 ? 1 : 0, true);


        if (gbuy)
            gustosProfit.buyMoment(price, time);
        if (gsell)
            gustosProfit.sellMoment(price, time);

//            helper().put(mldata, "prevProfits3", gustosProfit.lastProfits(3));
//            helper().put(mldata, "prevProfits5", gustosProfit.lastProfits(5));
//            helper().put(mldata, "prevProfit1", gustosProfit.lastProfit(0));
//            helper().put(mldata, "prevProfit2", gustosProfit.lastProfit(1));
//            helper().put(mldata, "prevProfit3", gustosProfit.lastProfit(2));
//            helper().put(mldata, "prevProfit4", gustosProfit.lastProfit(3));
//            helper().put(mldata, "prevProfit5", gustosProfit.lastProfit(4));
//
//            helper().put(mldata, "prevProfit1Time", gustosProfit.lastProfitTime(0, time));
//            helper().put(mldata, "prevProfit2Time", gustosProfit.lastProfitTime(1, time));
//            helper().put(mldata, "prevProfit3Time", gustosProfit.lastProfitTime(2, time));
//            helper().put(mldata, "prevProfit4Time", gustosProfit.lastProfitTime(3, time));
//            helper().put(mldata, "prevProfit5Time", gustosProfit.lastProfitTime(4, time));

        if (index > 0) {
            MarketBar marketBar = data.marketBars.get(index);
            if (marketBar.getEndTime().isAfter(bar.getEndTime()))
                System.out.println("!!!market bar after!");
            if (marketBar.getEndTime().isBefore(bar.getBeginTime().minusMinutes(2)))
                System.out.println("market bar before! " + Duration.between(marketBar.getEndTime(), bar.getBeginTime()).toMinutes() + " index=" + index);
            helper().put(mldata, "marketPos1", marketBar.p5m());
            helper().put(mldata, "marketPos2", marketBar.p15m());
            helper().put(mldata, "marketPos3", marketBar.p1h());
            helper().put(mldata, "marketPos4", marketBar.p12h());
            helper().put(mldata, "marketPos5", marketBar.p1d());
            helper().put(mldata, "marketPos6", marketBar.p3d());
//                helper().put(mldata,"marketPos7",marketBar.p7d());
//                helper().putLagged(mldata,"marketPos1",prevmldata2,2);
//                helper().putLagged(mldata,"marketPos2",prevmldata2,2);
//                helper().putLagged(mldata,"marketPos3",prevmldata2,2);
//                helper().put(mldata,"marketMax1",marketBar.max5m());
//                helper().put(mldata,"marketMax2",marketBar.max15m());
//                helper().put(mldata,"marketMax3",marketBar.max1h());
//                helper().put(mldata,"marketMax4",marketBar.max1d());
//                helper().put(mldata,"marketMax5",marketBar.max7d());
//                helper().put(mldata,"marketMin1",marketBar.min5m());
//                helper().put(mldata,"marketMin2",marketBar.min15m());
//                helper().put(mldata,"marketMin3",marketBar.min1h());
//                helper().put(mldata,"marketMin4",marketBar.min1d());
//                helper().put(mldata,"marketMin5",marketBar.min7d());
        }

        if (index>0) {
//            helper().addAvgAndDisp(data, "vdema0", index, 100, false, true);
//            helper().addAvgAndDisp(data, "vdema3", index, 100);
//            helper().addAvgAndDisp(data, "sd4", index, 100,false,true);
//            helper().addAvgAndDisp(data, "marketPos5", index, 100, true, false);
//            helper().addAvgAndDisp(data, "marketPos6", index, 100, true, false);
            helper().addAvgAndDisp(data, "marketPos5", index, 100, true, false);
//            helper().addAvgAndDispVolume(data, "volume", index, 100, 0, false, true);
//            helper().addAvgAndDispVolume(data, "volume", index, 300, 0, false, true);
//            helper().addAvgAndDispVolume(data, "volume", index, 1000, 0, false, true);

//            helper().addAvgAndDisp(data, "rsiv2", index, 100,true,true);
//            helper().addAvgAndDisp(data, "change4", index, 100);
//            helper().addAvgAndDisp(data, "stoh4", index, 100);
//            helper().addAvgAndDisp(data, "@goodBuy|main", index, 20, -1,true,false);
        }

        //            helper().put(mldata, "prevProfitShouldBuy",gustosProfit.shouldBuy()?1:0,true);
//            helper().put(mldata, "prevProfitSimpleTest", gustosProfit.simpleTest() ? 1 : 0, true);

//            helper().put(mldata,"hour",data.bar(index).getBeginTime().getHour());
//            helper().put(mldata,"day",sheet.bar(index).getBeginTime().getDayOfWeek().getValue());
//            helper().put(mldata,"holiday",sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.SUNDAY||sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.SATURDAY?1:0);
//            helper().put(mldata,"monday",sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.MONDAY?1:0);
//            helper().put(mldata,"friday",sheet.bar(index).getBeginTime().getDayOfWeek()== DayOfWeek.FRIDAY?1:0);

        if (index > calcAllFrom) {
            double targetPercent = 1 + 0.02;
            double stopLossPercent = 1 - 0.02;
            double target = price * targetPercent;
            double sl = price * stopLossPercent;


//            volumes.prepareForIntegral();
//            helper().put(mldata, "integralUp", volumes.integral(price, target));
//            helper().put(mldata, "integralUp2", volumes.integral(target, target * targetPercent));
//            helper().put(mldata, "integralDown", volumes.integral(sl, price));
//            helper().put(mldata, "integralDown2", volumes.integral(sl * stopLossPercent, sl));
//            helper().put(mldata, "ballanceOnBuy1", SheetUtils.volumesAroundLevel(data, price, index, 30));
//            helper().put(mldata, "ballanceOnBuy2", SheetUtils.volumesAroundLevel(data, price, index, 100));
//            helper().put(mldata, "ballanceOnBuy3", SheetUtils.volumesAroundLevel(data, price, index, 500));
//            helper().put(mldata, "ballanceOnTarget1", SheetUtils.volumesAroundLevel(data, target, index, 30));
//            helper().put(mldata, "ballanceOnTarget2", SheetUtils.volumesAroundLevel(data, target, index, 100));
//            helper().put(mldata, "ballanceOnTarget3", SheetUtils.volumesAroundLevel(data, target, index, 500));
//            helper().put(mldata, "ballanceOnSl1", SheetUtils.volumesAroundLevel(data, sl, index, 30));
//            helper().put(mldata, "ballanceOnSl2", SheetUtils.volumesAroundLevel(data, sl, index, 100));
//            helper().put(mldata, "ballanceOnSl3", SheetUtils.volumesAroundLevel(data, sl, index, 500));
//
//
//            helper().put(mldata, "upCandles1", SheetUtils.upCandles(data, index, 1));
//            helper().put(mldata, "upCandles2", SheetUtils.upCandles(data, index, 5));
//            helper().put(mldata, "upCandles3", SheetUtils.upCandles(data, index, 20));
//            helper().put(mldata, "downCandles1", SheetUtils.downCandles(data, index, 1));
//            helper().put(mldata, "downCandles2", SheetUtils.downCandles(data, index, 5));
//            helper().put(mldata, "downCandles3", SheetUtils.downCandles(data, index, 20));


//            doWithMinsMaxes(mldata, 5);
//            doWithMinsMaxes(mldata,10);

        }

    }


    private void calcTargetsDetector(int index) {
//        if (gsell) {
//            for (int i = targetCalcedTo; i < index; i++) {
//                MomentData mldata = data.buydata.get(i);
//                GustosLogicStrategy strategy;
//                Pair<Double, Integer> p;
//                int willKnow = 0;
//                strategy = new GustosLogicStrategy();
//                p = strategy.calcProfit(data, i);
//                willKnow = Math.max(willKnow, p.getSecond());
//                buyhelper().put(mldata, "_goodBuy", p.getFirst() > 1 ? 1.0 : 0, true);
//                mldata.weight = (p.getFirst() > 1 ? p.getFirst() - 1 : 1 / p.getFirst() - 1) * 100;
//
//
//                int nextSell = strategy.nextSell(data, i);
//                double nextPrice = nextSell >= data.size() ? price : data.bar(nextSell).getClosePrice();
//                double curPrice = data.bar(i).getClosePrice();
//                buyhelper().put(mldata, "_goodSell", curPrice > nextPrice ? 1.0 : 0, true);
//                willKnow = Math.max(willKnow, nextSell);
//                if (willKnow == Integer.MAX_VALUE) {
//                    targetCalcedTo = i;
//                    return;
//                }
//
//                mldata.whenWillKnow = data.bar(willKnow).getEndTime().toEpochSecond();
//            }
//
//            targetCalcedTo = index;
//        }

    }

    public void prepareGustosProfit() {
        gustosProfit = new PLHistory(null, null);
        for (int i = 0; i < calcIndex; i++) {
            XBar bar = data.bar(i);
            if (data.sells.get(i))
                gustosProfit.sellMoment(bar.getClosePrice(), bar.getEndTime().toEpochSecond());
            else if (data.buys.get(i))
                gustosProfit.buyMoment(bar.getClosePrice(), bar.getEndTime().toEpochSecond());

        }
    }

    public void prepareGoodSell(boolean withWeights) {
        int buyIndex = -1;
        double buyCost = 0;
        long buyTime = Long.MAX_VALUE;
        for (int i = targetCalcedTo; i >= 0; i--) {
            MomentData mldata = data.data.get(i);
            double cost = data.bar(i).getClosePrice();
            if (data.buys.get(i)) {
                buyIndex = i;
                buyCost = cost;
                buyTime = data.bar(i).getEndTime().toEpochSecond();
            }
            if (buyIndex < 0) continue;
            helper().put(mldata, "_goodSell", cost > buyCost ? 1 : 0, true);
            if (withWeights) {
                mldata.weight = (cost > buyCost ? cost / buyCost - 1 : buyCost / cost - 1) * 100;
                mldata.whenWillKnow = buyTime;
            }
        }
    }

    public void prepareGoodBuy() {
        int sellIndex = -1;
        long sellTime = Long.MAX_VALUE;
        double sellCost = 0;
        for (int i = targetCalcedTo; i >= 0; i--) {
            MomentData mldata = data.data.get(i);
            double cost = data.bar(i).getClosePrice();
            if (data.sells.get(i)) {
                sellIndex = i;
                sellCost = cost;
                sellTime = data.bar(i).getEndTime().toEpochSecond();
            }
            if (sellIndex < 0) continue;
            helper().put(mldata, "_goodBuy", cost < sellCost ? 1 : 0, true);
            mldata.weight = (cost < sellCost ? sellCost / cost - 1 : cost / sellCost - 1) * 100;
            mldata.whenWillKnow = sellTime;
        }
    }

    private void calcTargets(int index) {

        if (gsell) {
            for (int i = targetCalcedTo; i < index; i++) {
                MomentData mldata = data.data.get(i);
                MomentData mldatabuy = null;
                double curPrice = data.bar(i).getClosePrice();
                if (data.buydata != null)
                    mldatabuy = data.buydata.get(i);
                GustosLogicStrategy strategy;
                Pair<Double, Integer> p;
                int willKnow = 0;
                strategy = new GustosLogicStrategy();
                p = strategy.calcProfit(data, i);
                double max = strategy.maxPrice;
                double close = strategy.closePrice;
                willKnow = Math.max(willKnow, p.getSecond());
                double goodBuy = p.getFirst() > 1 ? 1.0 : 0;

                helper().put(mldata, "_goodBuy", goodBuy, true);
                mldata.weight = (p.getFirst() > 1 ? p.getFirst() - 1 : 1 / p.getFirst() - 1) * 100;

                helper().put(mldata, "_sellNow", (max - curPrice) < 0.05 * (max - close) ? 1 : 0, true);
//                mldata.weight2 = curPrice>close?curPrice/close:close/curPrice;

                manager.models.model.correctModelForMoment(i);



                int nextSell = strategy.nextSell(data, i);
                double nextPrice = nextSell >= data.size() ? price : data.bar(nextSell).getClosePrice();
                double goodSell = curPrice > nextPrice ? 1.0 : 0;
//                helper().put(mldata, "_goodSell", goodSell, true);
                willKnow = Math.max(willKnow, nextSell);
                if (willKnow == Integer.MAX_VALUE) {
                    targetCalcedTo = i;
                    return;
                }

                mldata.whenWillKnow = data.bar(willKnow).getEndTime().toEpochSecond();
                if (mldatabuy != null) {
                    buyhelper().put(mldatabuy, "_goodBuy", goodBuy, true);
                    buyhelper().put(mldatabuy, "_goodSell", goodSell, true);
                    mldatabuy.weight = mldata.weight;
                    mldatabuy.whenWillKnow = mldata.whenWillKnow;
                }

                if (manager.export != null && manager.export.size() > 0 && helper().get(mldata, "toAvgSd") < 0) {
                    Instance inst = helper().makeInstance(mldata, manager.ignoreBuy, manager.models.model.attFilter, 0, 9);
                    for (int j = 0; j < manager.export.size(); j++) {
                        Instances exam = manager.export.get(j).getSecond();
                        if (exam.size() < 5000)
                            exam.add(inst);
                    }
                }
                fillResult(i);
            }

            targetCalcedTo = index;
        }


    }

    private void fillResult(int index) {
        if (data.resultdata!=null) {
            MomentData rd = data.resultdata.get(index);
            for (int i = 0; i < 20; i++) {
                resulthelper().put(rd, "goodBuy" + i, helper().get(data.data.get(Math.max(0, index - i)), "@goodBuy|main",0), true);
            }
            MomentData md = data.data.get(index);
            resulthelper().put(rd, "_goodBuy", helper().get(md, "_goodBuy",0), true);
            resulthelper().put(rd, "price", data.bar(index).getClosePrice());
            rd.whenWillKnow = md.whenWillKnow;
            rd.weight = md.weight;
            rd.ignore = md.ignore;
        }

    }

    private void calcPredictionsDetector(int index) {

    }

    public void calcPredictions(int index) {
        if (manager.hasModel()) {
            MomentData mldata = data.data.get(index);

            FilterMomentsModel model = manager.models.model;
            helper().putResult(mldata, 0, "main", model.full && helper().classify(mldata, manager.ignore(true), model.attFilter, model.classifier, 0, 9));

            SellNowModel sellNowModel = manager.models.sellNowModel;
            helper().putResult(mldata, 1, "main", sellNowModel.classifier != null && model.full && helper().classify(mldata, manager.ignore(true), null, sellNowModel.classifier, 1, 9));


            boolean classifiedBuy = helper().get(mldata, "@goodBuy|main") > 0.5;
            boolean sellNow = helper().get(mldata, "@sellNow|main") > 0.5;
            boolean classifiedSell = true;//helper().get(mldata, "@goodSell|main") > 0.5;

            boolean gbuy = this.gbuy;
            boolean gsell = this.gsell;
//            boolean gbuy = buyhelper().classify(mldata2,manager.ignore(true), manager.models.model.models2.get(MAIN).get(0),0,9);
//            boolean gsell = buyhelper().classify(mldata2,manager.ignore(false), manager.models.model.models2.get(MAIN).get(1),1,9);


//            if (gbuy && manager.futuredata != null) {
//                Object classifier = manager.models.model.models.get(0);
//                if (classifier instanceof RandomForestWithExam) {
//                    RandomForestWithExam r = (RandomForestWithExam) classifier;
//                    Instance inst = helper().makeInstance(mldata, manager.ignoreBuy, 0, 9);
//                    Instances set = helper().makeEmptySet(manager.ignoreBuy, 0, 9);
//                    inst.setDataset(set);
//                    Pair<Double, Integer> pp = new GustosLogicStrategy().calcProfit(manager.futuredata, index);
//                    if (pp.getSecond() < manager.futuredata.size()) {
//                        inst.setValue(inst.classIndex(), pp.getFirst() > 1 ? 1 : 0);
//                        try {
//                            r.computeCombPizdunstvo(inst);
////                            double[][] p = r.computePizdunstvo(inst);
////                            double[][] p2 = r.computePizdunstvo2(inst);
////                            PizdunstvoData.pdbuy.add(set, data.instrument.toString(), (int) (time / (60 * 60 * 24 * 14)), p, p2);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }

//            if (gsell && futuredata != null) {
//                Object classifier = model.models.get(MAIN).get(1);
//                if (classifier instanceof RandomForestWithExam) {
//                    RandomForestWithExam r = (RandomForestWithExam) classifier;
//                    Instance inst = helper().makeInstance(mldata, ignoreSell, 1, 9);
//                    Instances set = helper().makeEmptySet(ignoreSell, 1, 9);
//                    inst.setDataset(set);
//                    int next = new GustosLogicStrategy().nextSell(futuredata, index);
//                    if (next < futuredata.size()) {
//                        inst.setValue(inst.classIndex(), futuredata.bar(calcIndex).getClosePrice() > futuredata.bar(next).getClosePrice() ? 1 : 0);
//                        try {
//                            double[][] p = r.computePizdunstvo(inst);
//                            double[][] p2 = r.computePizdunstvo2(inst);
//                            PizdunstvoData.pdsell.add(set, data.instrument.toString(), (int) (time / (60 * 60 * 24 * 14)), p, p2);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//
//            }


//            Pair<Integer, Integer> strategy = methods.chooseStrategy(time);

            if (gbuy) {
//                if (methods.wasNotNull()) {
                manager.plhistoryBase.buyMoment(price, time);
                if (classifiedBuy) {
                    manager.plhistoryClassifiedBuy.buyMoment(price, time);
                    manager.plhistoryClassifiedSelected.buyMoment(price, time);
                }

//                    if (strategy != null && (strategy.getFirst() == -1 || results[0][strategy.getFirst()]))
//                }

            }

            if (gsell) {
//                if (methods.wasNotNull()) {
                manager.plhistoryBase.sellMoment(price, time);
                manager.plhistoryClassifiedBuy.sellMoment(price, time);
                manager.plhistoryClassifiedSelected.sellMoment(price, time);
//                    if (strategy != null && (strategy.getSecond() == -1 || results[1][strategy.getSecond()]))
//                if (classifiedSell)
//                    manager.plhistoryClassifiedSelected.sellMoment(price, time);
//                }
            } else if (sellNow) {
                manager.plhistoryClassifiedSelected.sellMoment(price, time);

            }
            manager.plhistoryBase.minMaxCost(price,time);
            manager.plhistoryClassifiedBuy.minMaxCost(price,time);
            manager.plhistoryClassifiedSelected.minMaxCost(price,time);


        }

    }

    private double div(double v, double v1) {
        if (v1 == 0)
            return 0;
        return v / v1;
    }

}
