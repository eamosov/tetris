package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.util.Arrays;

public class GroupIndicator extends Indicator {
    private int[] ind;
    public GroupIndicator(IndicatorInitData data){
        super(data);
        ind = Arrays.stream(data.indicators.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {

        double[][] datas = new double[ind.length][];

        for (int i = 0;i<ind.length;i++)
            datas[i] = sheet.getData().get(ind[i]);

        int cur = -1;
        for (int i = 0; i < to; i++) {
            int ii = -1;
            for (int j = 0;j<ind.length;j++) {
                if (datas[j][i] == Indicator.YES) {
                    ii = j;
                    break;
                }
            }
            if (cur==-1 && ii>=0){
                cur = ii;
                values[0][i] = Indicator.YES;
            } else if (cur>=0 && ii>=0){
                cur = Math.min(cur,ii);
                values[0][i] = Indicator.YES;
            } else {
                values[0][i] = Indicator.NO;
                cur = -1;
            }


        }
    }

}

