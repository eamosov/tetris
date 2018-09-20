package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.indicators.Indicator;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class IndicatorsBottomPane extends JPanel {

    private Visualizator vis;

    public IndicatorsBottomPane(Visualizator vis) {
        this.vis = vis;
        vis.addListener(new VisualizatorViewListener() {
            @Override
            public void visualizatorViewChanged() {
                updatePrefSize();
                repaint();
            }
        });
    }

    private void updatePrefSize(){
//        Dimension d = getPreferredSize();
//        d.height = vis.candleWidth()* vis.getSheet().getLib().indicatorsShowBottom.size();
//        setPreferredSize(d);
//        invalidate();
    }

    public void paint(Graphics g){
        super.paint(g);
//        Sheet sheet = vis.getSheet();
//        if (sheet ==null) return;
//        int scale = vis.zoomScale();
//        int from = vis.getIndex();
//        int bars = getSize().width*scale/vis.candleWidth();
//        int to = Math.min(from + bars, sheet.size());
//        List<Indicator> ii = vis.getSheet().getLib().indicatorsShowBottom;
//        for (int j = 0;j<ii.size();j++) {
//            Indicator ind = ii.get(j);
//            Pair<Double,Double> minMax = SheetUtils.getIndicatorMinMax(sheet,ind,from,to,scale);
//            for (int i = from; i < to; i+=scale)
//                paintIndicator(g, i, j, scale, ind,minMax.getFirst(),minMax.getSecond());
//        }

    }

    private void paintIndicator(Graphics gg, int index, int indicatorIndex, int scale, Indicator ind, double min, double max) {
//        int w = vis.candleWidth();
//        int x = (index-vis.getIndex())/scale* w;
//        gg.setColor(VisUtils.NumberColor(vis.getSheet(), index, scale, ind, min, max));
//        gg.fillRect(x, indicatorIndex*w,w,w);
    }

    public String getIndicatorInfo(int index, Point p) {
        return "";
//        Indicator indicator = vis.getSheet().getLib().get(getIndicatorId(p));
//        return indicator.getName()+" "+vis.getSheet().getData().get(indicator,index)+" "+indicator.getMarks(index);
    }

    public int getIndicatorId(Point point) {
        return 0;
//        return vis.getSheet().getLib().indicatorsShowBottom.get(point.y/vis.candleWidth()).getId();
    }
}

