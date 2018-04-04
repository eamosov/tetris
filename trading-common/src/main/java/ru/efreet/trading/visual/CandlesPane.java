package ru.efreet.trading.visual;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;
import ru.efreet.trading.book.Sheet;
import ru.efreet.trading.book.SheetUtils;
import ru.efreet.trading.book.indicators.IIndicator;
import ru.efreet.trading.book.indicators.IndicatorType;

import javax.swing.*;
import java.awt.*;

public class CandlesPane extends JPanel {
    public static final Color RED = new Color(192, 0, 0);
    public static final Color GREEN = new Color(0, 192, 0);

    private Visualizator vis;
    private int indicator = -1;

    public CandlesPane(Visualizator vis) {
        this.vis = vis;
        vis.addListener(new VisualizatorViewListener() {
            @Override
            public void visualizatorViewChanged() {
                repaint();
            }
        });
    }

    public void paint(Graphics g){
        super.paint(g);
        Sheet sheet = vis.getSheet();
        if (sheet ==null) return;
        int from = vis.getIndex();
        int bars = getSize().width/vis.candleWidth();
        XBaseBar minMax = sheet.getSumBar(from, bars);
        paintHorizontalLines(g,minMax);
        int to = Math.min(from + bars, sheet.moments.size());
        for (int i = from; i< to; i++) {
            XBar bar = sheet.moments.get(i).bar;
            paintBar(g,i,bar,minMax);
        }
        if (indicator!=-1){
            IIndicator ii = vis.getSheet().getLib().listIndicators()[indicator];
            if (ii.getType()== IndicatorType.NUMBER){
                Pair<Double,Double> mm = SheetUtils.getIndicatorMinMax(sheet,ii,from,to);
                for (int i = from; i< to; i++)
                    paintIndicatorBar(g,i,ii,mm);

            }
        }
    }

    private void paintHorizontalLines(Graphics g, XBaseBar minMax) {
        double price = minMax.getMinPrice();
        g.setColor(new Color(208, 208, 208));
        do {
            int y = price2screen(price,minMax);
            g.drawLine(0,y,getWidth(),y);
            price = price*1.005;
        } while (price<=minMax.getMaxPrice());
    }

    private void paintIndicatorBar(Graphics g, int index, IIndicator indicator, Pair<Double, Double> mm) {
        double value = vis.getSheet().getData().get(indicator,index);
        if (Double.isNaN(value)) return;
        int w = vis.candleWidth();
        int x = (index-vis.getIndex())* w;
        Color col = VisUtils.NumberColor(vis.getSheet(),index, indicator, mm.getFirst(), mm.getSecond());
        g.setColor(col);
        if (indicator.fromZero()) {
            int h = (int) (getHeight() * 0.15 * (value - mm.getFirst()) / (mm.getSecond() - mm.getFirst()));
            g.fillRect(x, getHeight() - h, w, h);
        } else {

            double c = getHeight() * 0.075;
            if (value>0) {
                int h = (int) (c * value / mm.getSecond());
                g.fillRect(x, getHeight() - h- (int)c, w, h);
            } else {
                int h = (int) (c * value / mm.getFirst());
                g.fillRect(x, getHeight() - (int)c, w, h);

            }

        }

    }

    private int price2screen(double price, XBaseBar minMax){
        return getHeight()*8/10-(int)((price-minMax.getMinPrice())/(minMax.getMaxPrice()-minMax.getMinPrice())*getHeight()*0.7);
    }

    private void paintBar(Graphics g, int index, XBar bar, XBaseBar minMax) {
        int w = vis.candleWidth();
        int bound = 1;
        int x = (index-vis.getIndex())* w;
        g.setColor(Color.black);
        g.drawLine(x+ w /2,price2screen(bar.getMinPrice(),minMax),x+w/2,price2screen(bar.getMaxPrice(),minMax));
        g.setColor(bar.isBearish()?RED:GREEN);
        int lo = price2screen(Math.max(bar.getOpenPrice(), bar.getClosePrice()), minMax);
        int hi = price2screen(Math.min(bar.getOpenPrice(), bar.getClosePrice()), minMax);
        g.fillRect(x+bound, lo,w-bound*2,hi-lo+1);
    }

    public void setIndicator(int ind) {
        if (ind==indicator)
            indicator = -1;
        else
            indicator = ind;
        repaint();
    }
}
