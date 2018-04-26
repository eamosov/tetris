package ru.gustos.trading.book.indicators;

import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.bot.BotAdvice;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.logic.impl.LogicFactory;
import ru.efreet.trading.logic.impl.sd3.Sd3Logic;
import ru.efreet.trading.ta.indicators.XCachedIndicator;
import ru.efreet.trading.ta.indicators.XIndicator;
import ru.efreet.trading.ta.indicators.XPlusKIndicator;
import ru.gustos.trading.book.Sheet;

import java.awt.*;
import java.util.stream.Collectors;

public class Sd3NumberIndicator extends BaseIndicator {
    int ind;
    String param;

    public Sd3NumberIndicator(IndicatorInitData data) {
        super(data);
        ind = data.ind;
        param = data.param;
        show = data.show && !priceLine();
    }

    @Override
    public String getName() {
        return "sd3_"+ind+"_"+param;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public Color getColorMax() {
        return fromZero()?Color.green:Color.green;
    }


    @Override
    public boolean priceLine() {
        return "shortEma".equals(param) || "longEma".equals(param) || "sma".equals(param) || "dayShortEma".equals(param) || "dayLongEma".equals(param) || "tsl".equals(param) || "sd-".equals(param) || "sd+".equals(param);
    }

    static Color brown = new Color(92,92,16);
    @Override
    public Color getColorMin() {
        return fromZero()?brown:Color.red;
    }

    public XIndicator<XExtBar> values(Sheet sheet){
        Sd3Logic sd3 = (Sd3Logic)((EfreetIndicator)sheet.getLib().get(ind)).botLogic;
        switch (param){
            case "shortEma":
                return sd3.shortEma;
            case "longEma":
                return sd3.longEma;
            case "macd":
                return sd3.macd;
            case "signalEma":
                return sd3.signalEma;
            case "signal2Ema":
                return sd3.signal2Ema;
            case "sma":
                return sd3.sma;
            case "sd":
                return sd3.sd;
            case "sd+":
                return new XPlusKIndicator<>(sd3.sma,sd3.sd,sd3.getParams().getDeviation2()*0.1);
            case "sd-":
                return new XPlusKIndicator<>(sd3.sma,sd3.sd,-sd3.getParams().getDeviation()*0.1);
            case "dayShortEma":
                return sd3.dayShortEma;
            case "dayLongEma":
                return sd3.dayLongEma;
            case "dayMacd":
                return sd3.dayMacd;
            case "daySignalEma":
                return sd3.daySignalEma;
            case "daySignal2Ema":
                return sd3.daySignal2Ema;
            case "tsl":
                return sd3.tslIndicator;
        }
        return null;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {

        XIndicator<XExtBar> v = values(sheet);

        for (int i = 0; i < values.length; i++)
            values[i] = v.getValue(i);



    }
}
