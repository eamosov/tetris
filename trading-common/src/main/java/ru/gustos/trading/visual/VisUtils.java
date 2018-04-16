package ru.gustos.trading.visual;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.IIndicator;
import ru.gustos.trading.book.indicators.IndicatorType;

import java.awt.*;

public class VisUtils {

    public static Color lerp(Color c1, Color c2, double p){
        if (p<0) p = 0;
        if (p>1) p = 1;
        return new Color((int)(c2.getRed()*p + c1.getRed()*(1-p)),(int)(c2.getGreen()*p + c1.getGreen()*(1-p)),(int)(c2.getBlue()*p + c1.getBlue()*(1-p)));
    }

    public static Color NumberColor(Sheet sheet, int index, int scale, IIndicator ind, double min, double max) {
        int r = 0, g = 0, b = 0;
        for (int i = 0;i<scale;i++){
            Color color = VisUtils.NumberColor(sheet, index+i, ind, min, max);
            if (ind.getType()==IndicatorType.YESNO) {
                if (color!=Color.lightGray) {
                    r = Math.max(r, color.getRed());
                    g = Math.max(g, color.getGreen());
                    b = Math.max(b, color.getBlue());
                }
            } else {
                r += color.getRed();
                g += color.getGreen();
                b += color.getBlue();
            }
        }
        if (ind.getType()==IndicatorType.YESNO) {
            if (r==0 && g==0 && b==0) return Color.lightGray;
            return new Color(r, g, b);
        }else
            return new Color(r/scale,g/scale,b/scale);
    }

    public static Color NumberColor(Sheet sheet, int index, IIndicator ind, double min, double max) {
        Color col = Color.lightGray;
        double val = sheet.getData().get(ind,index);
        if (!Double.isNaN(val)) {
            if (ind.getType() == IndicatorType.YESNO) {
                if (val == IIndicator.YES)
                    col = ind.getColorMax();
                else if (val==IIndicator.NO)
                    col = ind.getColorMin();
            } else if (ind.getType() == IndicatorType.NUMBER && ind.fromZero()) {
                double p = (val-min)/(max-min);
                col = VisUtils.lerp(ind.getColorMin(),ind.getColorMax(),p);
            } else if (ind.getType() == IndicatorType.NUMBER && !ind.fromZero()) {
                if (val>0)
                    col =  ind.getColorMax();
                else if (val<0)
                    col = ind.getColorMin();
            }
        }
        return col;
    }
}
