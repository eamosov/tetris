package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.gustos.trading.book.BarsSource;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.Indicator;
import ru.gustos.trading.book.indicators.IndicatorResultType;
import ru.gustos.trading.book.indicators.VecUtils;

import java.awt.*;

public class VisUtils {

    public static Color lerp(Color c1, Color c2, double p){
        if (p<0) p = 0;
        if (p>1) p = 1;
        return new Color((int)(c2.getRed()*p + c1.getRed()*(1-p)),(int)(c2.getGreen()*p + c1.getGreen()*(1-p)),(int)(c2.getBlue()*p + c1.getBlue()*(1-p)));
    }

    public static Color NumberColor(BarsSource sheet, int index, int scale, Indicator ind, double min, double max) {
        return Color.lightGray;
//        if (ind.getResultType()== IndicatorResultType.YESNO) {
//            int v = sheet.getData().hasYesNo(ind,index,scale);
//            if (v==0) return Color.lightGray;
//            if (v==1) return ind.getColors().max();
//            if (v==2) return ind.getColors().min();
//            return lerp(ind.getColors().min(),ind.getColors().max(),0.5);
//        }else {
//            double val = sheet.getData().get(ind,index,scale);
//            return NumberColor(ind,val,min,max);
//        }
    }

    private static Color NumberColor(Indicator ind, double val, double min, double max) {
        Color col = Color.lightGray;
        if (!Double.isNaN(val)) {
            if (ind.getResultType() == IndicatorResultType.YESNO) {
                if (val == Indicator.YES)
                    col = ind.getColors().max();
                else if (val== Indicator.NO)
                    col = ind.getColors().min();
            } else if (ind.getResultType() == IndicatorResultType.NUMBER && ind.fromZero()) {
                double p = (val-min)/(max-min);
                col = VisUtils.lerp(ind.getColors().min(),ind.getColors().max(),p);
            } else if (ind.getResultType() == IndicatorResultType.NUMBER && !ind.fromZero()) {
                if (val>0)
                    col = ind.getColors().max();
                else if (val<0)
                    col = ind.getColors().min();
            }
        }
        return col;
    }

    public static void drawLine(Component c, Graphics g, double[] v, double margin){
        Pair<Double, Double> mm = VecUtils.minMax(v);
        for (int i = 1;i<v.length;i++){
            int y1 = (int)((1-margin-v[i-1]/mm.getSecond()*(1-margin*2))*c.getHeight());
            int y2 = (int)((1-margin-v[i]/mm.getSecond()*(1-margin*2))*c.getHeight());
            g.drawLine(i,y1,i+1,y2);
        }
    }

    public static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(),color.getGreen(),color.getBlue(),alpha);
    }
}
