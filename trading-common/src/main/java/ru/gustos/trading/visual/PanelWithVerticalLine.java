package ru.gustos.trading.visual;

import ru.gustos.trading.book.Extrapolation;
import ru.gustos.trading.global.BoundlinesFinder;
import ru.gustos.trading.global.LevelsAtPoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class PanelWithVerticalLine extends JPanel {
    private Visualizator vis;
    private Point p;
    private Popup popup = new Popup();

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
                if (button==3){
                    popup.show(PanelWithVerticalLine.this,p.x,p.y,vis.getIndexAt(p));
                }
            }
        });
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int units = e.getUnitsToScroll();
                int index = vis.getIndexAt(e.getPoint());
                if (units>0)
                    vis.zoomPlus(index);
                else
                    vis.zoomMinus(index);
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

    class Popup extends JPopupMenu{
        int index;
        double price;
        Popup(){
            JMenuItem levelLines = new JMenuItem("Level lines");
            add(levelLines);
            levelLines.addActionListener(l->vis.setLevels(new BoundlinesFinder(vis, index)));
            JMenuItem resetLevelLines = new JMenuItem("Reset level lines");
            add(resetLevelLines);
            resetLevelLines.addActionListener(l->vis.setLevels(null));
            addSeparator();

            JMenuItem priceLine = new JMenuItem("Price line");
            add(priceLine);
            priceLine.addActionListener(l->vis.setLineAtPrice(price));
            JMenuItem resetPriceLine = new JMenuItem("Reset price line");
            add(resetPriceLine);
            resetPriceLine.addActionListener(l->vis.setLineAtPrice(0));
            addSeparator();

            JMenuItem trainZones = new JMenuItem("Train zones");
            add(trainZones);
            trainZones.addActionListener(l->vis.enableTrainZones(index));

            addSeparator();

            JMenuItem marketPrices = new JMenuItem("Market prices");
            add(marketPrices);
            marketPrices.addActionListener(l->MarketPricesFrame.show(vis,index));
        }

        public void show(Component invoker, int x, int y, int index) {
            this.index = index;
            price = vis.frame.form.getCandlesPane().screen2price(y);
            super.show(invoker, x, y);
        }
    }
}

