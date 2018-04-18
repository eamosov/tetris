package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.indicators.IIndicator;
import ru.gustos.trading.book.indicators.IndicatorType;
import ru.efreet.trading.exchange.BarInterval;
import ru.gustos.trading.book.indicators.VecUtils;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CandlesPane extends JPanel {
    public static final Color RED = new Color(164, 32, 21);
    public static final Color GREEN = new Color(51, 147, 73);
    public static final Color BLUE = new Color(25, 25, 164);

    public static final Font gridFont = new Font("Dialog",Font.BOLD,12);
    public static final Color gridColor = new Color(242, 246, 246);
    public static final Color darkColor = new Color(74, 90, 90);
    public static final Color darkerColor = new Color(37, 45, 45);


    private Visualizator vis;

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
        int scale = vis.zoomScale();
        int bars = getSize().width* scale /vis.candleWidth();
        XBaseBar minMax = sheet.getSumBar(from, bars);
        int to = Math.min(from + bars, sheet.moments.size());
        if (vis.graphIndicator !=-1){
            IIndicator ii = vis.getSheet().getLib().get(vis.graphIndicator);
                Pair<Double,Double> mm = SheetUtils.getIndicatorMinMax(sheet,ii,from,to,scale);
                for (int i = from; i< to; i+=scale)
                    paintIndicatorBar(g,i,scale,ii,mm);
        }

        if (vis.backIndicator !=-1){
            IIndicator ii = vis.getSheet().getLib().get(vis.backIndicator);
                Pair<Double,Double> mm = SheetUtils.getIndicatorMinMax(sheet,ii,from,to,scale);
                for (int i = from; i< to; i+=scale)
                    paintIndicatorBar(g,i,scale,ii,mm);
        }

        paintGrid(g,minMax, from,false);
        for (int i = from; i< to; i+=scale) {
            XBar bar = getBar(i);
            paintBar(g,i,bar,minMax);
        }
        if (vis.param>0)
        paintVolumeLine(g);
        paintGrid(g,minMax, from,true);
    }

    private void paintVolumeLine(Graphics g) {
        Sheet sheet = vis.getSheet();
        int from = vis.getIndex();
        int scale = vis.zoomScale();
        int bars = getSize().width* scale /vis.candleWidth();
        int to = Math.min(from + bars, sheet.moments.size());
        double[] vv = new double[(to-from)/scale+2];
        double[] volumes = new double[(to-from)/scale];
        int j = 0;
        for (int i = from;i<to;i+=scale) {
            XBar b = sheet.getSumBar(i, scale);
            volumes[j++] = b.getVolume();
        }
        double avg = VecUtils.avg(volumes);
        double limit = vis.param*100;

        vv[0] = 0;
        j = 1;
        for (int i = from;i<to;i+=scale){
            XBar b = sheet.getSumBar(i,scale);
            if (b.getVolume()*Math.abs(b.deltaMaxMin())>limit) {
                double over = b.getVolume() - avg;
                vv[j] = vv[j-1]+(b.isBearish()?-over : over);
            } else
                vv[j] = vv[j-1];
            j++;
        }
        drawLine(g,vv);

    }

    private void drawLine(Graphics g, double[] vv) {
        double min = vv[0];
        double max = vv[0];
        for (int i = 1;i<vv.length;i++){
            if (vv[i]>max) max = vv[i];
            if (vv[i]<min) min = vv[i];
        }
        int w = vis.candleWidth();
        g.setColor(darkerColor);
        for (int i = 1;i<vv.length;i++) {
            int y1 = (int) ((0.9 - (vv[i - 1] - min) / (max - min)*0.8) * getHeight());
            int y2 = (int) ((0.9 - (vv[i] - min) / (max - min)*0.8) * getHeight());
            g.drawLine((i-1)* w, y1,i* w, y2);
        }
    }

    public XBar getBar(int index) {
        int scale = vis.zoomScale();
        Sheet sheet = vis.getSheet();
        if (scale ==1) return sheet.moments.get(index).bar;
        int from = index/scale*scale;
        XBaseBar bar = new XBaseBar(sheet.moments.get(from).bar);
        for (int j = 1;j<scale;j++) if (from+j<sheet.moments.size())
            bar.addBar(sheet.moments.get(from+j).bar);
        return bar;
    }

    private void paintGrid(Graphics g, XBaseBar minMax, int from, boolean text) {
        double price = minMax.getMinPrice();
        Graphics2D g2 = (Graphics2D)g;
        g2.setStroke(new BasicStroke(1.5f,1,1));
        if (text) {
            g.setFont(gridFont);
            g2.setColor(darkColor);
        } else {
            g2.setColor(gridColor);
        }
        double gridStep = 1.005;
        price = Math.pow(gridStep,(int)(Math.log(price)/Math.log(gridStep)));
        do {
            int y = price2screen(price,minMax);
            if (text)
                g.drawString(""+(int)price,getWidth()-40,y);
            else
                g.drawLine(0,y,getWidth(),y);
            price = price* gridStep;
        } while (price<=minMax.getMaxPrice());

        int w = vis.candleWidth();
        int x = 0;
        ZonedDateTime time;
        int scale = vis.zoomScale();

        do {
            time = vis.getSheet().moments.get(from+x*scale).bar.getBeginTime();
            if (x/20!=(x+1)/20) {
                if (text)
                    g.drawString(time.format(VisualizatorForm.dateFormatter),x*w,getHeight());
                else
                    g.drawLine(x*w, 0, x*w, getHeight());
            }
            x++;
        } while (x*w<getWidth() && x*scale+from<vis.getSheet().moments.size());
    }

    private void paintIndicatorBar(Graphics g, int index, int scale, IIndicator indicator, Pair<Double, Double> mm) {
        int w = vis.candleWidth();
        int x = (index-vis.getIndex())/scale* w;
        Color col = VisUtils.NumberColor(vis.getSheet(),index, scale,indicator, mm.getFirst(), mm.getSecond());
        if (indicator.getType()==IndicatorType.YESNO){
            g.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),60));
            g.fillRect(x, 0, w, getHeight());
        } else if (indicator.getType()==IndicatorType.NUMBER) {
            double value = vis.getSheet().getData().get(indicator,index,scale);
            if (Double.isNaN(value)) return;
            g.setColor(col);
            if (indicator.fromZero()) {
                double min = 0;//mm.getFirst();
                int h = (int) (getHeight() * 0.15 * (value - min) / (mm.getSecond() - mm.getFirst()));
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
        int x = (index-vis.getIndex())/vis.zoomScale()* w;
        g.setColor(darkColor);
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

}
