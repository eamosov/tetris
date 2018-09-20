package ru.gustos.trading.visual;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Moment;
import ru.gustos.trading.book.SheetUtils;

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
    private JLabel zoomLabel;
    private JButton leftToIndicator;
    private JButton rightToIndicator;
    private JTextField param;
    private JTextField avg;
    private JComboBox averageType;
    private JTextField vZoom;
    private JComboBox instruments;
    private JComboBox trades;

    private TimelinePanel timeline;
    private CandlesPane candles;
    private PriceVolumesPanel priceVolumes;
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

        priceVolumes = new PriceVolumesPanel(vis);
        panelWithLine.add(priceVolumes,BorderLayout.EAST);

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
            public void mouseDragged(MouseEvent e) { vis.mouseDrag(e.getPoint(),e.getButton(),e.getModifiers());  }

            @Override
            public void mouseMoved(MouseEvent e) {
                vis.mouseMove(e.getPoint());
            }
        });

        panelWithLine.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                vis.mouseClicked(e.getPoint(),e.getButton());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                vis.mousePressed(e.getPoint(),e.getButton());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                vis.mouseReleased(e.getPoint(),e.getButton());
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
            public void visualizatorMouseClicked(Point p, int button) {
                mouseClick(p);
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
        vZoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.setVerticalZoom(Integer.parseInt(vZoom.getText()));
            }
        });
        for (String name : vis.data.instruments)
            instruments.addItem(name);
        instruments.addActionListener(l->vis.setCurrent((String)instruments.getSelectedItem()));
        trades.addActionListener(l->vis.setShowTradeHistory(trades.getSelectedIndex()));

    }

    public void setZoom(int zoom){
        zoomLabel.setText("  Zoom: "+zoom+"  ");
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
//        ArrayList<Moment> mm = vis.getSheet().moments;
        if (index<0 || index>=vis.current().size())
            candles.setInfoText("");
        else {

            String info = info4bar(candles.getBar(index));
            vis.setSelectedIndex(index);
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
//        boolean has = vis.getSheet().getLib().indicatorsBack.size() > 0;
//        leftToIndicator.setEnabled(has);
//        rightToIndicator.setEnabled(has);
    }

    private String info4bar(XBar bar) {
        return String.format("time: %s, open: %s, close: %s, volume: %.2f, min: %s, max: %s",bar.getBeginTime().toString(), SheetUtils.price2string(vis.current(),bar.getOpenPrice()),SheetUtils.price2string(vis.current(),bar.getClosePrice()),bar.getVolume(),SheetUtils.price2string(vis.current(),bar.getMinPrice()),SheetUtils.price2string(vis.current(),bar.getMaxPrice()));
    }

    public CandlesPane getCandlesPane(){
        return candles;
    }

    private void viewUpdated() {
        if (!vis.current().instrument.toString().equals(instruments.getSelectedItem())){
            instruments.setSelectedItem(vis.current().instrument.toString());
        }
        indexField.setText(dateFormatter.format(vis.current().bar(vis.getIndex()).getBeginTime()).replace('T',' '));
    }

    private void TimeEntered() {
        String text = indexField.getText().trim().replace(' ','T');
        ZonedDateTime time = ZonedDateTime.of(LocalDateTime.parse(text, dateFormatter), ZoneId.of("UTC"));
        vis.setIndex(vis.current().getBarIndex(time));
    }

}

