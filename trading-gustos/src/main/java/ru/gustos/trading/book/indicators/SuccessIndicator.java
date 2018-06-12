package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SuccessIndicator extends Indicator implements IStringPropertyHolder {
    private Indicator indicator;
    private String filter = "";

    public SuccessIndicator(IndicatorInitData data){
        super(data);
    }

    public SuccessIndicator(int idd, int indd){
        this(new IndicatorInitData(){{this.id = idd;this.ind = indd;}});
    }

    @Override
    public Map<String,String> getMarks(int ind) {
        return indicator.getMarks(ind);
    }

    private boolean hasFilteredMark(Set<String> removeMarks, Map<String, String> marks){
        if (marks==null || marks.size()==0) return false;
        for (String key : marks.keySet())
            if (removeMarks.contains(key)) return true;
        return false;
    }

    @Override
    public IndicatorVisualType getVisualType() {
        return IndicatorVisualType.BACK;
    }

    @Override
    public IndicatorResultType getResultType() {
        return IndicatorResultType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        indicator = sheet.getLib().get(data.ind);

        double[] data = sheet.getData().get(this.data.ind);
        Set<String> removeMarks = Arrays.stream(filter.split(",")).collect(Collectors.toSet());
        removeMarks.remove("");
        double buyPrice = 0;
        int buyPos = 0;
        Map<String,String> buyKeys = null;
        for (int i = 0; i < to; i++) {

            double v = data[i];
            if (i==to-1)
                v = Indicator.NO;

            XBar bar = sheet.bar(i);
            if (v == Indicator.YES ){
                if (buyPos==0) {
                    buyPrice = bar.getClosePrice();
                    buyPos = i;
                    buyKeys = getMarks(i);
                }
            } else if (buyPos>0){
                double price = bar.getClosePrice();
                if (removeMarks.size()==0 || hasFilteredMark(removeMarks,buyKeys) || hasFilteredMark(removeMarks, getMarks(i))) {
//                    double result = price / buyPrice > 1.001 ? Indicator.YES : Indicator.NO;
                    for (int j = buyPos; j <= i; j++)
                        values[0][j] = price / buyPrice-1.001;
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
