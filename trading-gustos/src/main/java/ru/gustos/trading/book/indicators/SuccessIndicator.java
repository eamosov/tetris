package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SuccessIndicator extends BaseIndicator implements IStringPropertyHolder {
    private int ind;
    private IIndicator indicator;
    private String filter = "";

    public SuccessIndicator(IndicatorInitData data){
        super(data);
        ind = data.ind;
    }

    public SuccessIndicator(int id, int ind){
        super(id);
        this.ind = ind;
    }

    @Override
    public String getName() {
        return "success_"+ind;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public Color getColorMax() {
        return Color.green;
    }

    @Override
    public Color getColorMin() {
        return Color.red;
    }

    @Override
    public Map<String,String> getMark(int ind) {
        return indicator.getMark(ind);
    }

    private boolean hasFilteredMark(Set<String> removeMarks, Map<String, String> marks){
        if (marks==null || marks.size()==0) return false;
        for (String key : marks.keySet())
            if (removeMarks.contains(key)) return true;
        return false;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        indicator = sheet.getLib().get(ind);

        double[] data = sheet.getData().get(ind);
        Set<String> removeMarks = Arrays.stream(filter.split(",")).collect(Collectors.toSet());
        removeMarks.remove("");
        double buyPrice = 0;
        int buyPos = 0;
        Map<String,String> buyKeys = null;
        for (int i = 0; i < to; i++) {

            double v = data[i];
            if (i==to-1)
                v = IIndicator.NO;

            XBar bar = sheet.moments.get(i).bar;
            if (v == IIndicator.YES ){
                if (buyPos==0) {
                    buyPrice = bar.getClosePrice();
                    buyPos = i;
                    buyKeys = getMark(i);
                }
            } else if (buyPos>0){
                double price = bar.getClosePrice();
                if (removeMarks.size()==0 || hasFilteredMark(removeMarks,buyKeys) || hasFilteredMark(removeMarks,getMark(i))) {
                    double result = price / buyPrice > 1.001 ? IIndicator.YES : IIndicator.NO;
                    for (int j = buyPos; j <= i; j++)
                        values[j] = result;
                }
                buyKeys = null;
                buyPos = 0;
            }
        }
    }

    @Override
    public String getPropertyName() {
        return "Success filter";
    }

    @Override
    public String getPropertyValue() {
        return filter;
    }

    @Override
    public void setPropertyValue(String v) {
        filter = v;
    }
}
