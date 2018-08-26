package ru.gustos.trading.visual;

import ru.gustos.trading.book.Extrapolation;
import ru.gustos.trading.global.LevelsAtPoint;

import javax.swing.*;
import java.awt.*;

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
            public void visualizatorMouseClicked(Point p, int button) {
                if (vis.getLocalModelByClick()){

                    int index = vis.getIndexAt(p);
                    vis.setLevels(new LevelsAtPoint(vis.getSheet(), index, vis.zoomScale()*(vis.param==0?4:(int)vis.param)));
//                    Extrapolation ex = new Extrapolation(vis.getSheet(), index,vis.param==0?100:(int)vis.param);
//                    vis.setExtrapolation(ex);
                }
            }
        });
    }

    public void paint(Graphics g){
        super.paint(g);
        if (p==null) return;
        int index = vis.getIndexAt(p);
        int x = (index-vis.getIndex())/vis.zoomScale()*vis.candleWidth()+vis.candleWidth()/2;
        g.setColor(new Color(145,171,172,192));
        g.drawLine(x,0,x,getHeight());
        g.drawLine(0,p.y,getWidth()-36,p.y);
    }

}
