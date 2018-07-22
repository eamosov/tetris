package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.efreet.trading.bars.XBaseBar;
import ru.gustos.trading.book.Moment;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.indicators.Indicator;
import ru.gustos.trading.book.indicators.IndicatorResultType;
import ru.gustos.trading.book.indicators.VecUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

public class TimelinePanel extends JPanel implements MouseMotionListener, MouseListener{
    private Visualizator vis;

    public TimelinePanel(Visualizator vis) {
        this.vis = vis;
        Dimension d = getPreferredSize();
        d.height = 80;
        setPreferredSize(d);
        vis.addListener(new VisualizatorViewListener() {
            @Override
            public void visualizatorViewChanged() {
                repaint();
            }
        });
        addMouseMotionListener(this);
        addMouseListener(this);
    }

    public void paint(Graphics g){
        super.paint(g);
        int w = getWidth();
        ArrayList<Moment> mm = vis.getSheet().moments;

        int startIndex = vis.getIndex();
        int endIndex = vis.getEndIndex();
        int total = mm.size();

        int x1 = startIndex*w/total;
        int x2 = endIndex*w/total;
        g.setColor(Color.lightGray);
        g.fillRect(0,0,w,getHeight());
        g.setColor(Color.white);
        g.fillRect(x1,0,x2-x1,getHeight());
        g.setColor(CandlesPane.darkColor);
        g.drawLine(x1,0,x1,getHeight());
        g.drawLine(x2,0,x2,getHeight());


        g.setColor(CandlesPane.darkerColor);
        int prev = price2y(mm.get(0).bar.getClosePrice());
        for (int x = 1;x<w;x++){
            int y = price2y(mm.get(x*mm.size()/w).bar.getClosePrice());
            g.drawLine(x-1,prev,x,y);
            prev = y;
        }
        drawIndicatorLines(g);
        g.setColor(Color.blue);
        drawProfitLine(g);
        paintPriceAtLine(g);
    }

    private void paintPriceAtLine(Graphics g) {
        double price = vis.getLineAtPrice();
        if (price !=0){
            int y = price2y(price);
            g.setColor(CandlesPane.SELECTEDLINECOLOR);
            g.drawLine(0,y,getWidth(),y);
        }
        Pair<Double, Double> selectedPrice = vis.getSelectedPrice();
        if (selectedPrice!=null){
            int y1 = price2y(selectedPrice.getFirst());
            int y2 = price2y(selectedPrice.getSecond());
            g.setColor(CandlesPane.SELECTEDINTERVALCOLOR);
            g.fillRect(0,y2,getWidth(),y1-y2+1);
        }

    }


    private void drawProfitLine(Graphics g) {
        if (vis.playResult!=null) {

            double[] v = VecUtils.resize(vis.playResult.money,getWidth());
//            v = VecUtils.ma(v,10);
            VisUtils.drawLine(this,g,v,0.1);
        }
    }

    private void drawIndicatorLines(Graphics g) {
        ArrayList<Indicator> back = vis.getSheet().getLib().indicatorsBack;
        for (Indicator ind : back){
            int w = getWidth();
            int total = vis.getSheet().size();
            Color tmpcol = ind.getColors().max();
            Color colMax = new Color(tmpcol.getRed(),tmpcol.getGreen(),tmpcol.getBlue(),90);
            tmpcol = ind.getColors().min();
            Color colMin = new Color(tmpcol.getRed(),tmpcol.getGreen(),tmpcol.getBlue(),90);
            int alphaMin = 90, alphaMax = 90;
            Pair<Double, Double> mm = SheetUtils.getIndicatorMinMax(vis.getSheet(), ind, 0, vis.getSheet().size(), 1);
            double[] data = vis.getSheet().getData().get(ind.getId());
            for (int x = 0;x<w;x++) {
                int from = x*total/w;
                int to = (x+1)*total/w;
                double max = 0;
                double min = 0;
                for (int i = from;i<to;i++) {
                    double v = data[i];
                    if (v> max)
                        max = v;
                    if (v< min)
                        min = v;
                }

                if (ind.getResultType()==IndicatorResultType.NUMBER) {
                    if (min != 0)
                        alphaMin = (int) (40 + min * 160 / mm.getFirst());

                    if (max!=0)
                        alphaMax = (int) (40 + max * 160 / mm.getSecond());
                }

                if (min!=0 && max!=0){
                    g.setColor(VisUtils.alpha(colMax,alphaMax));
                    g.fillRect(x, 0, 1, getHeight()/2);
                    g.setColor(VisUtils.alpha(colMax,alphaMin));
                    g.fillRect(x, getHeight()/2, 1, getHeight()/2);
                } else if (min!=0 || max!=0){
                    Color col = max!=0?VisUtils.alpha(colMax,alphaMax):VisUtils.alpha(colMin,alphaMin);
                    g.setColor(col);
                    g.fillRect(x, 0, 1, getHeight());
                }
            }

        }

    }

    private int price2y(double price) {
        XBaseBar total = vis.getSheet().totalBar();
        return (int)((1-price/total.getMaxPrice())*getHeight());
    }


    @Override
    public void mouseClicked(MouseEvent e) {
        int pos = e.getPoint().x*vis.getSheet().size()/getWidth();
        vis.setMiddleIndex(pos);
    }

    Point dragStart = null;
    int indexStart;
    @Override
    public void mousePressed(MouseEvent e) {
        dragStart = e.getPoint();
        indexStart = vis.getIndex();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragStart = null;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
//        dragStart = null;
    }

    @Override
    public void mouseExited(MouseEvent e) {
//        dragStart = null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragStart!=null){
            int dx = e.getPoint().x-dragStart.x;
            int ind = indexStart + dx*vis.getSheet().size()/getWidth();
            vis.setIndex(ind);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        dragStart = null;
    }
}
