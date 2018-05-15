package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.indicators.IIndicator;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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
        d.height = vis.candleWidth()*vis.getSheet().getLib().listIndicatorsShow().size();
        setPreferredSize(d);
    }

    public void paint(Graphics g){
        super.paint(g);
        Sheet sheet = vis.getSheet();
        if (sheet ==null) return;
        int scale = vis.zoomScale();
        int from = vis.getIndex();
        int bars = getSize().width*scale/vis.candleWidth();
        int to = Math.min(from + bars, sheet.size());
        List<IIndicator> ii = vis.getSheet().getLib().listIndicatorsShow();
        for (int j = 0;j<ii.size();j++) {
            IIndicator ind = ii.get(j);
            Pair<Double,Double> minMax = SheetUtils.getIndicatorMinMax(sheet,ind,from,to,scale);
            for (int i = from; i < to; i+=scale)
                paintIndicator(g, i, j, scale, ind,minMax.getFirst(),minMax.getSecond());
        }

    }

    private void paintIndicator(Graphics gg, int index, int indicatorIndex, int scale, IIndicator ind, double min, double max) {
        int w = vis.candleWidth();
        int x = (index-vis.getIndex())/scale* w;
        gg.setColor(VisUtils.NumberColor(vis.getSheet(), index, scale, ind, min, max));
        gg.fillRect(x, indicatorIndex*w,w,w);
    }

    public String getIndicatorInfo(int index, Point p) {
        IIndicator indicator = vis.getSheet().getLib().get(getIndicatorId(p));
        return indicator.getName()+" "+vis.getSheet().getData().get(indicator,index)+" "+indicator.getMark(index);
    }

    public int getIndicatorId(Point point) {
        return vis.getSheet().getLib().listIndicatorsShow().get(point.y/vis.candleWidth()).getId();
    }
}

