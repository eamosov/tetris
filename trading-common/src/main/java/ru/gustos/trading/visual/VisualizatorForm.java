package ru.gustos.trading.visual;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Moment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class VisualizatorForm {
    private final DateTimeFormatter dateFormatter =DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private JPanel root;
    private JPanel top;
    private JButton left;
    private JButton right;
    private JPanel center;
    private JTextField indexField;
    private JLabel infoLabel;
    private JButton bot;
    private JButton zoomPlus;
    private JButton zoomMinus;
    private JLabel zoomLabel;

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
            public void mouseDragged(MouseEvent e) { vis.mouseDrag(e.getPoint());  }

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
                vis.mousePressed(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                vis.mouseReleased(e.getPoint());
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {
                vis.mouseExited(e.getPoint());
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
        bot.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new RuntimeException("FIX ME");
                //TOOD call RunBotDialog
//                RunBotDialog dlg = new RunBotDialog(vis);
//                dlg.pack();
//                dlg.setVisible(true);
            }
        });
        zoomPlus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.zoomPlus();
            }
        });
        zoomMinus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.zoomMinus();
            }
        });
        viewUpdated();
    }

    public void setZoom(int zoom){
        zoomLabel.setText("  Zoom level: "+zoom+"  ");
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

            String info = info4bar(candles.getBar(index));
            int indY = point.y-indicators.getLocation().y;

            if (indY>=0)
                info += "      "+indicators.getIndicatorInfo(index,new Point(point.x,indY));
            infoLabel.setText(info);
        }

    }

    private void mouseClick(Point point) {
        int indY = point.y-indicators.getLocation().y;
        if (indY>=0){
            int ind = indicators.getIndicatorId(new Point(point.x,indY));
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
        indexField.setText(dateFormatter.format(vis.getSheet().moments.get(vis.getIndex()).bar.getBeginTime()).replace('T',' '));
    }

    private void IndexEntered() {
        String text = indexField.getText().trim().replace(' ','T');
        ZonedDateTime time = LocalDate.parse(text, dateFormatter).atStartOfDay(ZoneId.of("UTC"));
        vis.setIndex(vis.getSheet().getBarIndex(time));
    }

}
