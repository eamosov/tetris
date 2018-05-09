package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.logic.impl.SimpleBotLogicParams;
import ru.gustos.trading.GustosBotLogic2;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;
import ru.gustos.trading.visual.VisUtils;

import java.awt.*;

public class Gustos3NumberIndicator extends BaseIndicator implements IStrokeProvider{
    int ind;
    String param;
    boolean length;
    boolean yesno;

    public Gustos3NumberIndicator(IndicatorInitData data) {
        super(data);
        ind = data.ind;
        param = data.param;
//        show = data.show && !priceLine();
        length = data.b1;
        yesno = data.b2;
    }

    @Override
    public String getName() {
        return "gustos3_"+ind+"_"+param+"_"+length+"_"+yesno;
    }

    @Override
    public IndicatorType getType() {
        return yesno?IndicatorType.YESNO:IndicatorType.NUMBER;
    }

    @Override
    public Color getColorMax() {
        if (priceLine()){
            switch (param) {
                case "sma":
                    return VisUtils.alpha(CandlesPane.GREEN,92);
                case "smaSell":
                    return VisUtils.alpha(CandlesPane.RED,92);
                case "sd-":
                    return CandlesPane.GREEN;
                case "sd-3":
                    return VisUtils.alpha(CandlesPane.GREEN,128);
                case "sdSell+":
                    return CandlesPane.RED;
                case "sdSell+3":
                    return VisUtils.alpha(CandlesPane.RED,128);

            }

        }
        return fromZero()?Color.green:Color.green;
    }

    @Override
    public float getStroke() {
        if (param.endsWith("3"))
            return 1f;
        if (param.startsWith("sma"))
            return 1.2f;
        return 1.7f;
    }

    @Override
    public boolean priceLine() {
        return "sma".equals(param) || "sd-".equals(param) || "sd-3".equals(param) || "sd+".equals(param) || "smaSell".equals(param) || "sdSell-".equals(param) || "sdSell+".equals(param) || "sdSell+3".equals(param);
    }

    static Color brown = new Color(92,92,16);
    @Override
    public Color getColorMin() {
        return fromZero()?brown:Color.red;
    }

    public double value(Sheet sheet, int index){
        GustosBotLogic2 bot = (GustosBotLogic2)((GustosIndicator)sheet.getLib().get(ind)).botLogic;
        XExtBar bar = bot.getBar(index);
        SimpleBotLogicParams params = bot.getParams();
        switch (param){
            case "sma":
                return bar.getSma();
            case "smaSell":
                return bar.getSmaSell();
            case "sd+":
                return bar.getSma() + bar.getSd()* params.getDeviation2()*0.1;
            case "sd-":
                return bar.getSma() - bar.getSd()* params.getDeviation()*0.1;
            case "sd-3":
                return bar.getSma() - bar.getSd()* params.getDeviation3()*0.1;
            case "sdSell+":
                return bar.getSmaSell() + bar.getSdSell()* params.getDeviation2()*0.1;
            case "sdSell+3":
                return bar.getSmaSell() + bar.getSdSell()* params.getDeviation3()*0.1;
        }
        return 0;
    }

    int lastTo = -1;
    int l = 0;
    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {

        if (!length) {
            for (int i = from; i < to; i++) {
                double vv = value(sheet,i);
                values[i] = yesno?(vv>0?IIndicator.YES:IIndicator.NO):vv;
            }
        }else {
            if (lastTo!=from) {
                l = 0;
                from = 0;
            }
            for (int i = from; i < to; i++) {
                double val = value(sheet,i);
                if (val>0)
                    l++;
                else
                    l = 0;
                values[i] = l;
            }
        }
        lastTo = to;

    }

}

