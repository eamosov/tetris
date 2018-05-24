package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

public class FileIndicator extends Indicator {
    public FileIndicator(IndicatorInitData data){
        super(data);
    }


    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {

        try {
            double[] v = VecUtils.fromFile("d:/tetrislibs/agents/"+data.state);
            System.arraycopy(v,from,values[0],from,to-from);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

