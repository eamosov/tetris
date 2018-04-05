package ru.gustos.trading.visual;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Moment;
import ru.gustos.trading.bots.CheatBot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class VisualizatorForm {
    private JPanel root;
    private JPanel top;
    private JButton left;
    private JButton right;
    private JPanel center;
    private JTextField indexField;
    private JLabel infoLabel;
    private JButton superBot;

    private CandlesPane candles;
    private IndicatorsPane indicators;

    private Visualizator vis;

    public VisualizatorForm(Visualizator vis) {
        this.vis = vis;
        left.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.goLeft();
            }
        });
        right.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.goRight();
            }
        });
        PanelWithVerticalLine panelWithLine = new PanelWithVerticalLine(vis);
        panelWithLine.setLayout(new BorderLayout());
        center.add(panelWithLine, BorderLayout.CENTER);

        candles = new CandlesPane(vis);
        panelWithLine.add(candles, BorderLayout.CENTER);

        indicators = new IndicatorsPane(vis);
        panelWithLine.add(indicators,BorderLayout.SOUTH);

        indexField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IndexEntered();
            }
        });
        center.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {            }

            @Override
            public void mouseMoved(MouseEvent e) {
                vis.mouseMove(e.getPoint());
            }
        });

        center.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                vis.mouseClicked(e.getPoint());
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        vis.addListener(new VisualizatorViewListener() {
            @Override
            public void visualizatorViewChanged() {
                viewUpdated();
            }
        });
        vis.addListener(new VisualizatorMouseListener() {
            @Override
            public void visualizatorMouseMoved(Point p) {
                mouseMoveCandles(p);
            }

            @Override
            public void visualizatorMouseClicked(Point p) {
                mouseClick(p);
            }
        });
        superBot.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new CheatBot(vis.getSheet()).run();
            }
        });
    }

    JPanel getCenter(){
        return center;
    }

    public JPanel getRoot(){
        return root;
    }

    private void mouseMoveCandles(Point point) {
        int index = vis.getIndexAt(point);
        ArrayList<Moment> mm = vis.getSheet().moments;
        if (index<0 || index>=mm.size())
            infoLabel.setText("");
        else {
            String info = info4bar(mm.get(index).bar);
            int indY = point.y-indicators.getLocation().y;

            if (indY>=0)
                info += "      "+indicators.getIndicatorValue(index,new Point(point.x,indY));
            infoLabel.setText(info);
        }

    }

    private void mouseClick(Point point) {
        int indY = point.y-indicators.getLocation().y;
        if (indY>=0){
            int ind = indicators.getIndicatorIndex(new Point(point.x,indY));
            candles.setIndicator(ind);
        }
    }

    private String info4bar(XBar bar) {
        return String.format("time: %s, open: %.2f, close: %.2f, volume: %.2f, min: %.2f, max: %.2f",bar.getBeginTime().toString(),bar.getOpenPrice(),bar.getClosePrice(),bar.getVolume(),bar.getMinPrice(),bar.getMaxPrice());
    }

    public CandlesPane getCandlesPane(){
        return candles;
    }

    private void viewUpdated() {
        indexField.setText(Integer.toString(vis.getIndex()));
    }

    private void IndexEntered() {
        vis.setIndex(Integer.parseInt(indexField.getText()));
    }

}
