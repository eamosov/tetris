package ru.gustos.trading.visual;

import ru.efreet.trading.bars.XBaseBar;
import ru.gustos.trading.book.Moment;
import ru.gustos.trading.book.indicators.IIndicator;
import ru.gustos.trading.book.indicators.IndicatorType;
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
    }

    private void drawProfitLine(Graphics g) {
        if (vis.playResult!=null && vis.backIndicator>=0) {

            double[] v = VecUtils.resize(vis.playResult.money,getWidth());
            v = VecUtils.ma(v,10);
            VisUtils.drawLine(this,g,v,0.1);
        }
    }

    private void drawIndicatorLines(Graphics g) {
        if (vis.backIndicator>=0){
            int w = getWidth();
            int total = vis.getSheet().moments.size();
            IIndicator ind = vis.getSheet().getLib().get(vis.backIndicator);
            Color tmpcol = ind.getColorMax();
            Color colMax = new Color(tmpcol.getRed(),tmpcol.getGreen(),tmpcol.getBlue(),90);
            tmpcol = ind.getColorMin();
            Color colMin = new Color(tmpcol.getRed(),tmpcol.getGreen(),tmpcol.getBlue(),90);

            double[] data = vis.getSheet().getData().get(vis.backIndicator);
            for (int x = 0;x<w;x++) {
                int from = x*total/w;
                int to = (x+1)*total/w;
                boolean hasYes = false;
                boolean hasNo = false;
                for (int i = from;i<to;i++) {
                    double v = data[i];
                    if (v== IIndicator.YES)
                        hasYes = true;
                    if (v== IIndicator.NO)
                        hasNo = true;
                }

                if (hasYes && hasNo){
                    g.setColor(colMax);
                    g.fillRect(x, 0, 1, getHeight()/2);
                    g.setColor(colMin);
                    g.fillRect(x, getHeight()/2, 1, getHeight()/2);
                } else if (hasYes || hasNo){
                    Color col = hasYes?colMax:colMin;
                    g.setColor(col);
                    g.fillRect(x, 0, 1, getHeight());
                }
            }

        }

    }

    private int price2y(double price) {
        XBaseBar total = vis.getSheet().totalBar();
        return (int)((1-(price-total.getMinPrice())/total.deltaMaxMin())*getHeight());
    }


    @Override
    public void mouseClicked(MouseEvent e) {
        int pos = e.getPoint().x*vis.getSheet().moments.size()/getWidth();
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
            int ind = indexStart + dx*vis.getSheet().moments.size()/getWidth();
            vis.setIndex(ind);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        dragStart = null;
    }
}
