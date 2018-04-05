package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.indicators.IIndicator;

import javax.swing.*;
import java.awt.*;

public class IndicatorsPane extends JPanel {

    private Visualizator vis;

    public IndicatorsPane(Visualizator vis) {
        this.vis = vis;
        vis.addListener(new VisualizatorViewListener() {
            @Override
            public void visualizatorViewChanged() {
                repaint();
            }
        });
        Dimension d = getPreferredSize();
        d.height = vis.candleWidth()*vis.getSheet().getLib().listIndicators().length;
        setPreferredSize(d);
    }

    public void paint(Graphics g){
        super.paint(g);
        Sheet sheet = vis.getSheet();
        if (sheet ==null) return;
        int from = vis.getIndex();
        int bars = getSize().width/vis.candleWidth();
        IIndicator[] ii = vis.getSheet().getLib().listIndicators();
        for (int j = 0;j<ii.length;j++) {
            IIndicator ind = ii[j];
            int to = Math.min(from + bars, sheet.moments.size());
            Pair<Double,Double> minMax = SheetUtils.getIndicatorMinMax(sheet,ind,from,to);
            for (int i = from; i < to; i++)
                paintIndicator(g, i, j, ind,minMax.getFirst(),minMax.getSecond());
        }

    }

    private void paintIndicator(Graphics g, int index, int indicatorIndex, IIndicator ind, double min, double max) {
        int w = vis.candleWidth();
        int x = (index-vis.getIndex())* w;
        g.setColor(VisUtils.NumberColor(vis.getSheet(),index, ind, min, max));
        g.fillRect(x, indicatorIndex*w,w,w);
    }

    public String getIndicatorValue(int index, Point p) {
        IIndicator indicator = vis.getSheet().getLib().listIndicators()[getIndicatorIndex(p)];
        return indicator.getName()+" "+vis.getSheet().getData().get(indicator,index);
    }

    public int getIndicatorIndex(Point point) {
        return point.y/vis.candleWidth();
    }
}

