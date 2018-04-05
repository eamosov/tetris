package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.indicators.IIndicator;
import ru.gustos.trading.book.indicators.IndicatorType;
import ru.efreet.trading.exchange.BarInterval;

import javax.swing.*;
import java.awt.*;
import java.time.ZonedDateTime;

public class CandlesPane extends JPanel {
    public static final Color RED = new Color(164, 32, 21);
    public static final Color GREEN = new Color(51, 147, 73);

    private Visualizator vis;
    private int indicator = -1;

    public CandlesPane(Visualizator vis) {
        this.vis = vis;
        setBackground(Color.white);
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
        int to = Math.min(from + bars, sheet.moments.size());

        if (indicator!=-1){
            IIndicator ii = vis.getSheet().getLib().listIndicators()[indicator];
//            if (ii.getType()== IndicatorType.NUMBER){
                Pair<Double,Double> mm = SheetUtils.getIndicatorMinMax(sheet,ii,from,to);
                for (int i = from; i< to; i++)
                    paintIndicatorBar(g,i,ii,mm);

//            }
        }

        paintGrid(g,minMax, sheet.moments.get(from).bar.getBeginTime(),sheet.interval());
        for (int i = from; i< to; i++) {
            XBar bar = sheet.moments.get(i).bar;
            paintBar(g,i,bar,minMax);
        }
    }

    private void paintGrid(Graphics g, XBaseBar minMax, ZonedDateTime time, BarInterval interval) {
        double price = minMax.getMinPrice();
        Graphics2D g2 = (Graphics2D)g;
        g2.setColor(new Color(242, 246, 246));
        g2.setStroke(new BasicStroke(1.5f,1,1));

        do {
            int y = price2screen(price,minMax);
            g.drawLine(0,y,getWidth(),y);
            price = price*1.005;
        } while (price<=minMax.getMaxPrice());

        int w = vis.candleWidth();
        int x = 0;
        ZonedDateTime prevTime = time.minus(interval.getDuration());
        do {
            if (time.getHour()!=prevTime.getHour()) {
                g.drawLine(x, 0, x, getHeight());
            }
            x+=w;
            prevTime = time;
            time = time.plus(interval.getDuration());
        } while (x<getWidth());

    }

    private void paintIndicatorBar(Graphics g, int index, IIndicator indicator, Pair<Double, Double> mm) {
        double value = vis.getSheet().getData().get(indicator,index);
        if (Double.isNaN(value)) return;
        int w = vis.candleWidth();
        int x = (index-vis.getIndex())* w;
        Color col = VisUtils.NumberColor(vis.getSheet(),index, indicator, mm.getFirst(), mm.getSecond());
        if (indicator.getType()==IndicatorType.YESNO){
            g.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),30));
            g.fillRect(x, 0, w, getHeight());
        } else if (indicator.getType()==IndicatorType.NUMBER) {
            g.setColor(col);
            if (indicator.fromZero()) {
                int h = (int) (getHeight() * 0.15 * (value - mm.getFirst()) / (mm.getSecond() - mm.getFirst()));
                g.fillRect(x, getHeight() - h, w, h);
            } else {

                double c = getHeight() * 0.075;
                if (value > 0) {
                    int h = (int) (c * value / mm.getSecond());
                    g.fillRect(x, getHeight() - h - (int) c, w, h);
                } else {
                    int h = (int) (c * value / mm.getFirst());
                    g.fillRect(x, getHeight() - (int) c, w, h);

                }

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
        g.setColor(new Color(74,90,90));
        g.drawLine(x+ w /2,price2screen(bar.getMinPrice(),minMax),x+w/2,price2screen(bar.getMaxPrice(),minMax));
        int lo = price2screen(Math.max(bar.getOpenPrice(), bar.getClosePrice()), minMax);
        int hi = price2screen(Math.min(bar.getOpenPrice(), bar.getClosePrice()), minMax);
        g.setColor((bar.isBearish()?RED:GREEN).brighter());
        g.fillRect(x+bound, lo,w-bound*2,hi-lo+1);
        if (hi-lo-1>0) {
            g.setColor(bar.isBearish() ? RED : GREEN);
            g.fillRect(x + bound + 1, lo + 1, w - bound * 2 - 2, hi - lo - 1);
        }
    }

    public void setIndicator(int ind) {
        if (ind==indicator)
            indicator = -1;
        else
            indicator = ind;
        repaint();
    }
}
