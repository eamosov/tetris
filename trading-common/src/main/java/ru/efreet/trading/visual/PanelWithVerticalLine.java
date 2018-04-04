package ru.efreet.trading.visual;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class PanelWithVerticalLine extends JPanel {
    private Visualizator vis;
    private Point p;

    public PanelWithVerticalLine(Visualizator vis){
        this.vis = vis;
        vis.addListener(new VisualizatorMouseListener() {
            @Override
            public void visualizatorMouseMoved(Point pm) {
                 p = pm;
                 repaint();
            }

            @Override
            public void visualizatorMouseClicked(Point p) {

            }
        });
    }

    public void paint(Graphics g){
        super.paint(g);
        if (p==null) return;
        int index = vis.getIndexAt(p);
        int x = (index-vis.getIndex())*vis.candleWidth()+vis.candleWidth()/2;
        g.setColor(Color.darkGray);
        g.drawLine(x,0,x,getHeight());
    }

}
