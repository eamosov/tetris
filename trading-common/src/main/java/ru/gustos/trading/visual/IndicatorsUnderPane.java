package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.indicators.Indicator;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class IndicatorsUnderPane extends JPanel {
    static final int H = 80;
    private Visualizator vis;

    public IndicatorsUnderPane(Visualizator vis) {
        this.vis = vis;
        vis.addListener(() -> {
            updatePrefSize();
            repaint();
        });
    }

    private void updatePrefSize(){
//        Dimension d = getPreferredSize();
//        d.height = H* vis.getSheet().getLib().indicatorsUnder.size();
//        setPreferredSize(d);
//        revalidate();
//        getParent().revalidate();
    }

    public void paint(Graphics g){
        super.paint(g);
//        Sheet sheet = vis.getSheet();
//        if (sheet ==null) return;
//        int scale = vis.zoomScale();
//        int from = vis.getIndex();
//        int bars = getSize().width*scale/vis.candleWidth();
//        int to = Math.min(from + bars, sheet.size());
//        List<Indicator> ii = vis.getSheet().getLib().indicatorsUnder;
//        for (int j = 0;j<ii.size();j++) {
//            Indicator ind = ii.get(j);
//            Pair<Double,Double> minMax = SheetUtils.getIndicatorMinMax(sheet,ind,from,to,scale);
//            for (int i = from; i < to; i+=scale)
//                paintIndicatorBar(g, j, i, scale, ind, minMax);
//        }

    }

    private void paintIndicatorBar(Graphics g, int n, int index, int scale, Indicator indicator, Pair<Double, Double> mm) {
//        double value = vis.getSheet().getData().get(indicator,index,scale);
//        if (Double.isNaN(value)) return;
//
//        int w = vis.candleWidth();
//        int x = (index-vis.getIndex())/scale* w;
//        Color col = VisUtils.NumberColor(vis.getSheet(),index, scale,indicator, mm.getFirst(), mm.getSecond());
//
//        g.setColor(col);
//        if (indicator.fromZero()) {
//            double min = 0;//mm.getFirst();
//            int h = (int) (H * (value - min) / (mm.getSecond() - mm.getFirst()));
//            g.fillRect(x, H - h + n*H, w, h);
//        } else {
//
//            double c = H / 2;
//            if (value > 0) {
//                int h = (int) (c * value / mm.getSecond());
//                g.fillRect(x, H - h - (int) c+n*H, w, h);
//            } else {
//                int h = (int) (c * value / mm.getFirst());
//                g.fillRect(x, H - (int) c+n*H, w, h);
//            }
//        }

    }

}
