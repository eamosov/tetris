package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.indicators.*;

import javax.swing.*;
import java.awt.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;

public class CandlesPane extends JPanel {
    public static final Color RED = new Color(164, 32, 21);
    public static final Color GREEN = new Color(51, 147, 73);
    public static final Color BLUE = new Color(25, 25, 164);

    public static final Font gridFont = new Font("Dialog", Font.BOLD, 12);
    public static final Color gridColor = new Color(242, 246, 246,128);
    public static final Color darkColor = new Color(74, 90, 90);
    public static final Color darkerColor = new Color(37, 45, 45);

    public static final double gridStep = 1.005;


    private Visualizator vis;
    private JLabel infoLabel;

    public CandlesPane(Visualizator vis) {
        this.vis = vis;
        setOpaque(false);
        infoLabel = new JLabel();
        add(infoLabel);
        setBackground(Color.white);
        vis.addListener(new VisualizatorViewListener() {
            @Override
            public void visualizatorViewChanged() {
                repaint();
            }
        });
    }

    private XBaseBar minMax;
    private int from;
    private int scale;
    private int bars;
    private int to;

    private void prepare(){
        Sheet sheet = vis.getSheet();
        from = vis.getIndex();
        scale = vis.zoomScale();
        bars = getSize().width * scale / vis.candleWidth();
        to = Math.min(from + bars, sheet.size());
        if (vis.getFullZoom())
            minMax = sheet.totalBar();
        else
            minMax = sheet.getSumBar(from, bars);
    }

    public void paint(Graphics g) {
        Sheet sheet = vis.getSheet();
        if (sheet == null) return;
        boolean hasAverage = vis.averageWindow > 0 && !vis.averageType.equalsIgnoreCase("None");
        prepare();
        if (hasAverage) {
            prepareAverage();
            minMax = VecUtils.expandMinMax(minMax, avg, disp, 2.1, from, bars);
        }

        for (Indicator ii : vis.getSheet().getLib().indicatorsPrice)
            for (int i = 0; i < ii.getNumberOfLines(); i++)
                minMax = VecUtils.expandMinMax(minMax, vis.getSheet().getData().getLine(ii.getId(), i), null, 2.1, from, bars);



        paintBackIndicators(g);
        paintGrid(g, false);
//        paintVolumes(g);
        for (int i = from; i < to; i += scale) {
            XBar bar = getBar(i);
            paintBar(g, i, bar);
        }
        if (vis.param > 0)
            paintVolumeLine(g);
        paintGrid(g, true);
        if (hasAverage)
            paintAverage(g);
        for (Indicator ii : vis.getSheet().getLib().indicatorsPrice)
            for (int i = 0; i < ii.getNumberOfLines(); i++)
                paintPriceLine(g, ii, i);
        minMax = null;
        super.paint(g);
    }

    private void paintBackIndicators(Graphics g) {
        paintForceMap(g);

        ArrayList<Indicator> back = vis.getSheet().getLib().indicatorsBack;
        for (Indicator ii : back) {
            Pair<Double, Double> mm = SheetUtils.getIndicatorMinMax(vis.getSheet(), ii, 0, vis.getSheet().size(), 1);
            prevMark = null;
            for (int i = from; i < to; i += scale)
                paintBackIndicator(g, i, scale, ii, mm);
        }

    }

    private void paintForceMap(Graphics g) {
        Indicator indicator = vis.getSheet().getLib().get(13);
        if (indicator!=null) {
            Object o = indicator.getCoreObject();
            if (o instanceof GustosVolumeLevel2) {
                GustosVolumeLevel2 core = (GustosVolumeLevel2) o;
                Pair<Double, Double> mm = new Pair<>(Double.MAX_VALUE, Double.MIN_VALUE);
                for (int i = from; i < to; i++) {
                    double[] map = core.forcemap(i);
                    mm = VecUtils.minMax(map, mm);
                }
                for (int i = from; i < to; i += scale) {
                    paintForceMapBar(g, i, scale, core, mm);
                }

            }
        }
    }

    private void paintForceMapBar(Graphics g, int index, int scale, GustosVolumeLevel2 core, Pair<Double, Double> mm) {
        int w = vis.candleWidth();
        int x = (index - vis.getIndex()) / scale * w;
        double[] m = core.forcemap(index).clone();
        int cc = 1;
        for (int i = 1;i<scale;i++) if (index+i<vis.getSheet().size()) {
            m = VecUtils.add(m, core.forcemap(index + i), 1);
            cc++;
        }
        if (cc>1)
            VecUtils.mul(m,1.0/cc);
        double price = vis.getSheet().bar(index).middlePrice();
        for (int i = 0;i<m.length;i++){
            double pp = price+price*0.002*(i-m.length/2-0.5);
            double pp2 = price+price*0.002*(i-m.length/2+0.5);
            int col = (int)(255*(m[i]-mm.getFirst())/(mm.getSecond()-mm.getFirst()));
            if (col<0) col = 0;
            if (col>255) col = 255;
            g.setColor(new Color(255-col,255-col,255-col));

            g.fillRect(x, price2screen(pp), w, price2screen(pp)-price2screen(pp2)+1);
        }

    }

    private void paintPriceLine(Graphics g, Indicator ii, int line) {
        paintPriceLine(g, vis.getSheet().getData().getLine(ii.getId(), line), ii.getColors().lineColor(line), ii.getColors().stroke(line));
    }

    private int prevWindow = -1;
    private String prevAvgType = "";
    private double[] avg;
    private double[] disp;

    private void prepareAverage() {
        Sheet sheet = vis.getSheet();
        int window = vis.averageWindow;
        if (window != prevWindow || !prevAvgType.equals(vis.averageType)) {
            double[] v = sheet.moments.stream().mapToDouble(m -> m.bar.getClosePrice()).toArray();
            double[] vols = sheet.moments.stream().mapToDouble(m -> m.bar.getVolume()).toArray();
            Pair<double[], double[]> rr;
            if (vis.averageType.equalsIgnoreCase("gustos"))
                rr = GustosAverageRecurrent.calc(v, window, vols, window * 4);
            else if (vis.averageType.equalsIgnoreCase("gustos2"))
                rr = GustosAverageRecurrent2.calc(v, window, vols, window * 4);
            else if (vis.averageType.equalsIgnoreCase("Real avg"))
                rr = VecUtils.futureMaAndDisp(v, window);
            else if (vis.averageType.equalsIgnoreCase("gustos ema"))
                rr = VecUtils.gustosEmaAndDisp(v, window, vols, window * 4);
            else if (vis.averageType.equalsIgnoreCase("gustos ema2"))
                rr = VecUtils.gustosEmaAndDisp(v, window, vols, window * 4);
            else
                rr = VecUtils.emaAndMed(v, window);
//            Pair<double[], double[]> rr = VecUtils.gustosEmaAndDisp(v, window, vols, window*4);
//            Pair<double[], double[]> rr2 = VecUtils.emaAndMed(v, window);
            avg = rr.getFirst();
            disp = rr.getSecond();
//            for (int i = 0;i<disp.length;i++)
//                disp[i] = Math.min(disp[i],rr2.getSecond()[i]);
            prevWindow = window;
            prevAvgType = vis.averageType;
        }

    }

    private void paintAverage(Graphics g) {
        int scale = vis.zoomScale();
        paintPriceLine(g, this.avg, BLUE, 2f);
//        paintPriceLine(g, VecUtils.add(this.avg, disp, 1), darkColor, 1f);
//        paintPriceLine(g, VecUtils.add(this.avg, disp, -1), darkColor, 1f);

    }

    private void paintPriceLine(Graphics g, double[] values, Color color, float stroke) {
        int from = vis.getIndex();
        int scale = vis.zoomScale();
        int bars = getSize().width * scale / vis.candleWidth();
        int w = vis.candleWidth();
        double a = VecUtils.avg(values, from, scale);
        int ya = price2screen(a);
        ((Graphics2D) g).setStroke(new BasicStroke(stroke));
        g.setColor(color);
        for (int i = 1; i < bars; i++) {
            a = VecUtils.avg(values, from + i * scale, scale);
            int y = price2screen(a);
            g.drawLine((i - 1) * w+w/2, ya, i * w+w/2, y);
            ya = y;
        }

    }

    private void paintVolumes(Graphics g) {
        int steps = 100;
        double[] vv = vis.getSheet().calcVolumes(vis.getIndex() + vis.barsOnScreen() / 2, vis.barsOnScreen() / 2, minMax.getMinPrice(), minMax.getMaxPrice(), steps);
        Pair<Double, Double> mm = VecUtils.minMax(vv);
        double price = minMax.getMinPrice();
        double step = (minMax.getMaxPrice() - price) / steps;
        for (int i = 0; i < vv.length; i++) {
            int y = price2screen(price);
            price += step;
            int ny = price2screen(price);
            int r = (int) (vv[i] * 255 / mm.getSecond());
            g.setColor(new Color(r, 0, 0));
            g.fillRect(getWidth() / 2 - 30, y, 60, y - ny + 1);
        }
    }

    private void paintVolumeLine(Graphics g) {
        Sheet sheet = vis.getSheet();
        int from = vis.getIndex();
        int scale = vis.zoomScale();
        int bars = getSize().width * scale / vis.candleWidth();
        int to = Math.min(from + bars, sheet.size());
        double[] vv = new double[(to - from) / scale + 2];
        double[] volumes = new double[(to - from) / scale];
        int j = 0;
        for (int i = from; i < to; i += scale) {
            XBar b = sheet.getSumBar(i, scale);
            volumes[j++] = b.getVolume();
        }
        double avg = VecUtils.avg(volumes);
        double limit = vis.param * 100;

        vv[0] = 0;
        j = 1;
        for (int i = from; i < to; i += scale) {
            XBar b = sheet.getSumBar(i, scale);
            if (b.getVolume() * Math.abs(b.deltaMaxMin()) > limit) {
                double over = b.getVolume() - avg;
                vv[j] = vv[j - 1] + (b.isBearish() ? -over : over);
            } else
                vv[j] = vv[j - 1];
            j++;
        }
        drawLine(g, vv);

    }

    private void drawLine(Graphics g, double[] vv) {
        double min = vv[0];
        double max = vv[0];
        for (int i = 1; i < vv.length; i++) {
            if (vv[i] > max) max = vv[i];
            if (vv[i] < min) min = vv[i];
        }
        int w = vis.candleWidth();
        g.setColor(darkerColor);
        for (int i = 1; i < vv.length; i++) {
            int y1 = (int) ((0.9 - (vv[i - 1] - min) / (max - min) * 0.8) * getHeight());
            int y2 = (int) ((0.9 - (vv[i] - min) / (max - min) * 0.8) * getHeight());
            g.drawLine((i - 1) * w, y1, i * w, y2);
        }
    }

    public XBar getBar(int index) {
        int scale = vis.zoomScale();
        Sheet sheet = vis.getSheet();
        if (scale == 1) return sheet.bar(index);
        int from = index / scale * scale;
        XBaseBar bar = new XBaseBar(sheet.moments.get(from).bar);
        for (int j = 1; j < scale; j++)
            if (from + j < sheet.size())
                bar.addBar(sheet.moments.get(from + j).bar);
        return bar;
    }

    private void paintGrid(Graphics g, boolean text) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(1.5f, 1, 1));
        if (text) {
            g.setFont(gridFont);
            g2.setColor(darkColor);
        } else {
            g2.setColor(gridColor);
        }
        if (!text) {
            double price = screen2price(getHeight() * 9 / 10);
            double maxprice = screen2price(getHeight() / 10);
            price = gridPrice(price);
            if (price <= 1) price = 1;
            do {
                int y = price2screen(price);
                g.drawLine(0, y, getWidth(), y);
                price = price * gridStep;
            } while (price <= maxprice);
        }
        int w = vis.candleWidth();
        int x = 0;
        ZonedDateTime time;
        int scale = vis.zoomScale();

        do {
            time = vis.getSheet().moments.get(from + x * scale).bar.getBeginTime();
            if (x / 20 != (x + 1) / 20) {
                if (text)
                    g.drawString(time.format(VisualizatorForm.dateFormatter), x * w, getHeight());
                else
                    g.drawLine(x * w, 0, x * w, getHeight());
            }
            x++;
        } while (x * w < getWidth() && x * scale + from < vis.getSheet().size());
    }

    private String prevMark = null;

    private void paintBackIndicator(Graphics g, int index, int scale, Indicator indicator, Pair<Double, Double> mm) {
        int w = vis.candleWidth();
        int x = (index - vis.getIndex()) / scale * w;
        Color col = VisUtils.NumberColor(vis.getSheet(), index, scale, indicator, mm.getFirst(), mm.getSecond());
        int alpha = 40;
        if (indicator.getResultType() == IndicatorResultType.NUMBER) {
            double val = vis.getSheet().getData().get(indicator, index, scale);
            if (indicator.fromZero()) {
                alpha = (int) (20 + val * 100 / mm.getSecond());
            } else {
                if (val > 0)
                    alpha = (int) (20 + val * 100 / mm.getSecond());
                else
                    alpha = (int) (20 + val * 100 / mm.getFirst());
            }
        }
        if (alpha<20) alpha = 20;
        if (alpha>120) alpha = 120;
        g.setColor(VisUtils.alpha(col, alpha));
        g.fillRect(x, 0, w, getHeight());
        Map<String, String> markMap = indicator.getMarks(index);
        String mark = markMap == null ? "" : markMap.toString();
        g.setColor(darkerColor);
        if (mark != null && !mark.equals(prevMark)) {
            g.drawString(mark, x, 35);
            prevMark = mark;
        }
    }

    private double margin(){
        int zoom = vis.getVZoom();
        double margin = 0.1+zoom*0.05;
        if (margin<0.1) margin = 0.1;
        if (margin>0.4) margin = 0.4;
        return margin;
    }

    public int price2screen(double price) {
        if (minMax==null) prepare();
        double margin = margin();
        return (int)(getHeight() * ((0.9-margin) - (price - minMax.getMinPrice()) / (minMax.getMaxPrice() - minMax.getMinPrice()) * (0.9-margin*2)));
    }

    public double screen2price(int y){
        if (minMax==null) prepare();
        double margin = margin();
        return ((0.9-margin) - y*1.0/getHeight())/(0.9-margin*2)*(minMax.getMaxPrice() - minMax.getMinPrice())+minMax.getMinPrice();
    }

    public static double gridPrice(double price) {
        return SheetUtils.gridPrice(price,gridStep);
    }
    private void paintBar(Graphics g, int index, XBar bar) {
        int w = vis.candleWidth();
        int bound = 1;
        int x = (index - vis.getIndex()) / vis.zoomScale() * w;
        g.setColor(darkColor);
        g.drawLine(x + w / 2, price2screen(bar.getMinPrice()), x + w / 2, price2screen(bar.getMaxPrice()));
        int lo = price2screen(Math.max(bar.getOpenPrice(), bar.getClosePrice()));
        int hi = price2screen(Math.min(bar.getOpenPrice(), bar.getClosePrice()));
        g.setColor((bar.isBearish() ? RED : GREEN).brighter());
        g.fillRect(x + bound, lo, w - bound * 2, hi - lo + 1);
        if (hi - lo - 1 > 0) {
            g.setColor(bar.isBearish() ? RED : GREEN);
            g.fillRect(x + bound + 1, lo + 1, w - bound * 2 - 2, hi - lo - 1);
        }
    }

    public void setInfoText(String s) {
        infoLabel.setText(s);
    }

}

