package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.GoodBad;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.Volumes;
import ru.gustos.trading.book.indicators.ColorScheme;
import ru.gustos.trading.book.indicators.VecUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static ru.gustos.trading.visual.CandlesPane.darkColor;
import static ru.gustos.trading.visual.CandlesPane.gridFont;
import static ru.gustos.trading.visual.CandlesPane.gridStep;

public class PriceVolumesPanel extends JPanel  {
    public static final int W = 200;
    Visualizator vis;
    double minprice, maxprice;
    double priceAtMouse;


    public PriceVolumesPanel(Visualizator vis) {
        this.vis = vis;
        updatePrefSize();
        vis.addListener(new VisualizatorViewListener() {
            @Override
            public void visualizatorViewChanged() {
                repaint();
            }
        });

        vis.addListener(new VisualizatorBarAtMouseListener() {
            @Override
            public void visualizatorBarAtMouseChanged(int index) {
                repaint();
            }
        });


        vis.addListener(new VisualizatorMouseListener() {
            @Override
            public void visualizatorMouseMoved(Point p) {
                priceAtMouse = vis.frame.form.getCandlesPane().screen2price(p.y);
                repaint();
            }

            @Override
            public void visualizatorMouseClicked(Point p, int button) {
                if (vis.getPriceLineByClick()) {
                    if (button == 1)
                        vis.setLineAtPrice(vis.frame.form.getCandlesPane().screen2price(p.y));
                    else
                        vis.setLineAtPrice(0);
                }
            }
        });

    }

    private CandlesPane candlesPane() {
        return vis.frame.form.getCandlesPane();
    }

    private void updatePrefSize() {
        Dimension d = getPreferredSize();
        d.width = W;
        setPreferredSize(d);
    }

    public void paint(Graphics g) {
        super.paint(g);
        CandlesPane candles = candlesPane();
        minprice = candles.screen2price(getHeight() * 9 / 10);
        maxprice = candles.screen2price(getHeight() / 10);
        minprice = Math.max(minprice,vis.getSheet().totalBar().getMinPrice());
        maxprice = Math.min(maxprice,vis.getSheet().totalBar().getMaxPrice());
        paintVolumes(g);
        paintPriceAtLine(g);
        paintPrices(g);
        paintSelectedPrice(g);

    }

    private void paintSelectedPrice(Graphics g) {
        int y = candlesPane().price2screen(priceAtMouse);
        g.setColor(Color.lightGray);
        g.fillRect(W-40,y-5,W,15);
        g.setColor(Color.black);
        g.drawString(SheetUtils.price2string(vis.getSheet(),priceAtMouse),W-40,y+5);
    }

    private void paintVolumes(Graphics g) {
        CandlesPane candles = candlesPane();
        Volumes vols = vis.getSheet().volumes();
        GoodBad goodbad = vis.getSheet().goodbad();

        int minpow = vols.price2pow(minprice);
        int maxpow = vols.price2pow(maxprice);
        int index = vis.getSelectedIndex();
        XBar selectedBar = vis.getSheet().bar(index);
        ZonedDateTime time = selectedBar.getEndTime();
        Pair<double[], double[]> vv;
//        if (vis.getGustosVolumes()){
//        if (vis.getGustosVolumes())
//            vv = vols.getGustosVolumes(time);
//        else
        if (vis.getFixedVolumes())
            vv = vols.get();
        else
            vv = vols.getMoment(time);

        Pair<double[], double[]> goodbadvv = goodbad.getMoment(time);
        double[] goodbadsum = VecUtils.add(goodbadvv.getFirst(), goodbadvv.getSecond(), 1);
        double goodbadsummax = IntStream.range(minpow, maxpow + 1).mapToDouble(i -> i < 0 || i >= goodbadsum.length ? 0 : goodbadsum[i]).max().getAsDouble();

        double[] vvBase = smooth(vv.getFirst());
        double[] vvAsset = smooth(vv.getSecond());
        double maxvol = IntStream.range(minpow, maxpow + 1).mapToDouble(i -> i < 0 || i >= vvBase.length ? 0 : vvBase[i] + vvAsset[i]).max().getAsDouble();
        double[] sum = smooth(VecUtils.add(vv.getFirst(),vv.getSecond(),1));
        int[] levels = VecUtils.listLevels(sum,vis.param==0?36:vis.param,12);
        if (levels.length==0) return;
        int levelIndex = VecUtils.findBaseInLevels(levels,minpow);
        for (int i = minpow; i <= maxpow; i++) {
            if (levelIndex+1<levels.length && levels[levelIndex+1]==i)
                levelIndex++;
            double price = vols.pow2price(i);
            double volAsset = i < 0 || i >= vvBase.length ? 0 : vvAsset[i];
            double volBase = i < 0 || i >= vvBase.length ? 0 : vvBase[i];
            double goodbadV = i < 0 || i >= vvBase.length ? 0 : goodbadsum[i];
            int y = candles.price2screen(price);
            int nexty = pow2screen(i + 1);
            int h = y - nexty;
            if (h < 1) h = 1;
            int wasset = (int) ((W - 40) * volAsset / maxvol)/2;
            int wbase = (int) ((W - 40) * volBase / maxvol)/2;
//                g.setColor(price>=selectedBar.getClosePrice()?Color.green.darker():Color.green);
            Color color = CandlesPane.GREEN;
//            if (VecUtils.integrals.get(levelIndex)==0 && VecUtils.integrals.get(levelIndex+2)==0)
//                color = color.darker();
            g.setColor(color);
            g.fillRect(W - 40 - wasset, y - h / 2, wasset, h + 1);
//                g.setColor(price<=selectedBar.getClosePrice()?Color.red.darker():Color.red);
            g.setColor(color);
            g.fillRect(W - 40 - wbase-wasset, y - h / 2, wbase, h + 1);
            g.setColor(CandlesPane.BLUE);
            g.fillRect(0, y - h / 2, (int)((W-40)/2*goodbadV/goodbadsummax), h + 1);
//            g.fillRect(0, y - h / 2, wbase, h + 1);
        }

        g.setColor(Color.black);
        for (int i = 0;i<levels.length;i++){
            int p = levels[i];
//            g.drawLine(0,pow2screen(p),W,pow2screen(p));
        }
        int powClose = vols.price2pow(selectedBar.getClosePrice());
        levelIndex = VecUtils.findBaseInLevels(levels,powClose);

        int level = levels[levelIndex];
        int upperlevel = VecUtils.nextLevel(levels,levelIndex,1, vols.steps);
        int upperlevel2 = VecUtils.nextLevel(levels,levelIndex,2, vols.steps);
        int upperlevel3 = VecUtils.nextLevel(levels,levelIndex,3, vols.steps);
        int lowerlevel = VecUtils.nextLevel(levels,levelIndex,-1, vols.steps);

        g.setColor(Color.blue);

//        g.drawLine(0,pow2screen(level),W-40,pow2screen(level));
//        g.drawLine(0,pow2screen(level)-1,W-40,pow2screen(level)-1);
//        g.drawLine(0,pow2screen(lowerlevel),W-40,pow2screen(lowerlevel));
//        g.drawLine(0,pow2screen(upperlevel),W-40,pow2screen(upperlevel));
//        g.drawLine(0,pow2screen(upperlevel2),W-40,pow2screen(upperlevel2));
//        g.drawLine(0,pow2screen(upperlevel3),W-40,pow2screen(upperlevel3));

        int up1 = vis.getSheet().whenPriceWas(index-1,vols.pow2price(upperlevel));
        int down1 = vis.getSheet().whenPriceWas(index-1,vols.pow2price(level));
//        System.out.println((index-up1)+" "+(index-down1));

    }

    private void paintPriceAtLine(Graphics g) {
        double price = vis.getLineAtPrice();
        if (price !=0){
            int y = candlesPane().price2screen(price);
            g.setColor(CandlesPane.SELECTEDLINECOLOR);
            g.drawLine(0,y,getWidth(),y);
        }
        Pair<Double, Double> selectedPrice = vis.getSelectedPrice();
        if (selectedPrice!=null){
            int y1 = candlesPane().price2screen(selectedPrice.getFirst());
            int y2 = candlesPane().price2screen(selectedPrice.getSecond());
            g.setColor(CandlesPane.SELECTEDINTERVALCOLOR);
            g.fillRect(0,y2,getWidth(),y1-y2+1);
        }
    }



    private double[] smooth(double[] v){
        return v;//VecUtils.ma(v,4);

    }

    private int pow2screen(int p){
        return candlesPane().price2screen(vis.getSheet().volumes().pow2price(p));
    }


    private void paintPrices(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g.setFont(gridFont);
        g2.setColor(darkColor);
        double grid = gridStep;
        CandlesPane candles = candlesPane();
        double price = CandlesPane.gridPrice(minprice);
        int prevy = -100;
        do {
            int y = candles.price2screen(price);
            if (Math.abs(y - prevy) > 50) {
                g.drawString(SheetUtils.price2string(vis.getSheet(), price), getWidth() - 36, y);
                prevy = y;
            }
            price = price * grid;
        } while (price <= maxprice);

    }


}
