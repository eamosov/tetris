package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.logic.impl.sd5.Sd5Logic;
import ru.efreet.trading.ta.indicators.*;
import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class Sd3NumberIndicator  {
//    int ind;
//    String param;
//    boolean length;
//    boolean yesno;
//
//    public Sd3NumberIndicator(IndicatorInitData data) {
//        super(data);
//        ind = data.ind;
//        param = data.param;
//        show = data.show && !priceLine();
//        length = data.b1;
//        yesno = data.b2;
//    }
//
//    @Override
//    public String getName() {
//        return "sd3_"+ind+"_"+param+"_"+length+"_"+yesno;
//    }
//
//    @Override
//    public IndicatorResultType getResultType() {
//        return yesno? IndicatorResultType.YESNO: IndicatorResultType.NUMBER;
//    }
//
//    @Override
//    public Color getColorMax() {
//        return fromZero()?Color.green:Color.green;
//    }
//
//
//    @Override
//    public boolean priceLine() {
//        return "shortEma".equals(param) || "longEma".equals(param) || "sma".equals(param) || "dayShortEma".equals(param) || "dayLongEma".equals(param) || "tsl".equals(param) || "sd-".equals(param) || "sd+".equals(param);
//    }
//
//    static Color brown = new Color(92,92,16);
//    @Override
//    public Color getColorMin() {
//        return fromZero()?brown:Color.red;
//    }
//
//    public XIndicator<XExtBar> values(Sheet sheet){
//        Sd5Logic sd3 = (Sd5Logic)((EfreetIndicator)sheet.getLib().get(ind)).botLogic;
//        switch (param){
//            case "shortEma":
//                return sd3.shortEma;
//            case "longEma":
//                return sd3.longEma;
//            case "macd":
//                return sd3.macd;
//            case "signalEma":
//                return sd3.signalEma;
//            case "signal2Ema":
//                return sd3.signal2Ema;
//            case "sma":
//                return sd3.sma;
//            case "smaSell":
//                return sd3.sma;
//            case "sd+":
//                return new XPlusKIndicator<>(sd3.sma,sd3.sd,sd3.getParams().getDeviation2()*0.1);
//            case "sd-":
//                return new XPlusKIndicator<>(sd3.sma,sd3.sd,-sd3.getParams().getDeviation()*0.1);
//            case "dayShortEma":
//                return sd3.dayShortEma;
//            case "dayLongEma":
//                return sd3.dayLongEma;
//            case "dayMacd":
//                return sd3.dayMacd;
//            case "daySignalEma":
//                return sd3.daySignalEma;
//            case "daySignal2Ema":
//                return sd3.daySignal2Ema;
//            case "tsl":
//                return sd3.tslIndicator;
//            case "dayDownTrend":
//                return new XMinusIndicator<>(sd3.dayMacd,sd3.daySignal2Ema);
//            case "localUpTrend":
//                return new XMinusIndicator<>(sd3.macd,sd3.signalEma);
//            case "localDownTrend":
//                return new XMinusIndicator<>(sd3.macd,sd3.signal2Ema);
//            case "dayUpTrend":
//                return new XMinusIndicator<>(sd3.dayMacd,sd3.daySignalEma);
//        }
//        return null;
//    }
//
//    int lastTo = -1;
//    int l = 0;
//    @Override
//    public void calcValues(Sheet sheet, double[] values, int from, int to) {
//
//        XIndicator<XExtBar> v = values(sheet);
//        if (!length) {
//            for (int i = from; i < to; i++) {
//                double vv = v.getValue(i);
//                values[i] = yesno?(vv>0? Indicator.YES: Indicator.NO):vv;
//            }
//        }else {
//            if (lastTo!=from) {
//                l = 0;
//                from = 0;
//            }
//            for (int i = from; i < to; i++) {
//                double val = v.getValue(i);
//                if (val>0)
//                    l++;
//                else
//                    l = 0;
//                values[i] = l;
//            }
//        }
//        lastTo = to;
//
//    }
}

