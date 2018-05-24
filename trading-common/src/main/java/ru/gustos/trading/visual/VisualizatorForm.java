package ru.gustos.trading.visual;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Moment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class VisualizatorForm {
    public static final DateTimeFormatter dateFormatter =DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private JPanel root;
    private JPanel top;
    private JButton left;
    private JButton right;
    private JPanel center;
    private JTextField indexField;
    private JButton bot;
    private JButton zoomPlus;
    private JButton zoomMinus;
    private JLabel zoomLabel;
    private JButton leftToIndicator;
    private JButton rightToIndicator;
    private JTextField param;
    private JButton runButton;
    private JTextField avg;
    private JComboBox averageType;
    private JButton indicatorsButton;

    private TimelinePanel timeline;
    private CandlesPane candles;
    private IndicatorsBottomPane indicatorsBottom;
    private IndicatorsUnderPane indicatorsUnder;

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
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        PanelWithVerticalLine panelWithLine = new PanelWithVerticalLine(vis);
        panelWithLine.setLayout(new BorderLayout());
        center.add(split, BorderLayout.CENTER);

        timeline = new TimelinePanel(vis);
        split.setTopComponent(timeline);
        split.setBottomComponent(panelWithLine);

        candles = new CandlesPane(vis);
        panelWithLine.add(candles, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        panelWithLine.add(south,BorderLayout.SOUTH);
        indicatorsUnder = new IndicatorsUnderPane(vis);
        indicatorsBottom = new IndicatorsBottomPane(vis);
        south.add(indicatorsUnder,BorderLayout.CENTER);
        south.add(indicatorsBottom,BorderLayout.SOUTH);


        indexField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TimeEntered();
            }
        });
        panelWithLine.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) { vis.mouseDrag(e.getPoint());  }

            @Override
            public void mouseMoved(MouseEvent e) {
                vis.mouseMove(e.getPoint());
            }
        });

        panelWithLine.addMouseListener(new MouseListener() {
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
//        bot.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                throw new RuntimeException("FIX ME");
//                //TOOD call RunBotDialog
////                RunBotDialog dlg = new RunBotDialog(vis);
////                dlg.pack();
////                dlg.setVisible(true);
//            }
//        });
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
        leftToIndicator.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.moveToIndicator(-1);
            }
        });
        rightToIndicator.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.moveToIndicator(1);
            }
        });
        updateScrollToIndicators();
        param.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.setParam(Double.parseDouble(param.getText()));
            }
        });
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.runPlay();
            }
        });
        avg.setText(Integer.toString(vis.averageWindow));
        averageType.setSelectedItem(vis.averageType);
        avg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.setAverage((String)averageType.getSelectedItem(),Integer.parseInt(avg.getText()));
            }
        });
        averageType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.setAverage((String)averageType.getSelectedItem(),Integer.parseInt(avg.getText()));

            }
        });
        indicatorsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.onShowIndicators();
            }
        });
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
        setInfo(index, point);
    }

    public void setInfo(int index, Point point) {
        ArrayList<Moment> mm = vis.getSheet().moments;
        if (index<0 || index>=mm.size())
            candles.setInfoText("");
        else {

            String info = info4bar(candles.getBar(index));
            if (point!=null) {

                int indY = candles.getLocationOnScreen().y+point.y - indicatorsBottom.getLocationOnScreen().y;

                if (indY >= 0)
                    info += "      " + indicatorsBottom.getIndicatorInfo(index, new Point(point.x, indY));
            }
            candles.setInfoText(info);
        }
    }

    private void mouseClick(Point point) {
        int indY = candles.getLocationOnScreen().y + point.y- indicatorsBottom.getLocationOnScreen().y;
        if (indY>=0){
            int ind = indicatorsBottom.getIndicatorId(new Point(point.x,indY));
            vis.updateSelectedIndicator(ind);
            updateScrollToIndicators();
        }
    }

    void updateScrollToIndicators() {
        boolean has = vis.getSheet().getLib().indicatorsBack.size() > 0;
        leftToIndicator.setEnabled(has);
        rightToIndicator.setEnabled(has);
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

    private void TimeEntered() {
        String text = indexField.getText().trim().replace(' ','T');
        ZonedDateTime time = ZonedDateTime.of(LocalDateTime.parse(text, dateFormatter), ZoneId.of("UTC"));
        vis.setIndex(vis.getSheet().getBarIndex(time));
    }

}
