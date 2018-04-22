package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

import java.awt.*;
import java.util.Arrays;

public class GroupIndicator extends BaseIndicator{
    private int[] ind;
    public GroupIndicator(IndicatorInitData data){
        super(data);
        ind = Arrays.stream(data.indicators.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    @Override
    public String getName() {
        return "group_"+Arrays.toString(ind);
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
    public void calcValues(Sheet sheet, double[] values) {

        double[][] datas = new double[ind.length][];

        for (int i = 0;i<ind.length;i++)
            datas[i] = sheet.getData().get(ind[i]);

        int cur = -1;
        for (int i = 0; i < values.length; i++) {
            int ii = -1;
            for (int j = 0;j<ind.length;j++) {
                if (datas[j][i] == IIndicator.YES) {
                    ii = j;
                    break;
                }
            }
            if (cur==-1 && ii>=0){
                cur = ii;
                values[i] = IIndicator.YES;
            } else if (cur>=0 && ii<=cur){
                cur = Math.min(cur,ii);
                values[i] = IIndicator.YES;
            } else {
                values[i] = IIndicator.NO;
                cur = -1;
            }


        }
    }

}

