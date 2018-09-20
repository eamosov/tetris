package ru.gustos.trading.visual;

import javafx.stage.Screen;
import org.jfree.chart.*;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import ru.gustos.trading.global.InstrumentData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;

public class MarketPricesFrame extends JFrame implements ChartMouseListener {
    static MarketPricesFrame frame;
    ChartPanel panel;
    Visualizator vis;

    public MarketPricesFrame(){
        super("Market prices");
        setDefaultCloseOperation(HIDE_ON_CLOSE);
    }

    public static void show(Visualizator vis, int index) {
        if (frame==null)
            frame = new MarketPricesFrame();
        frame.set(vis,index);

    }

    private void set(Visualizator vis, int index) {
        this.vis = vis;
        TimeSeriesCollection all = new TimeSeriesCollection();
        int fromEnd = vis.current().size()-index;
        for (int i = 0;i<vis.data.data.size();i++){
            InstrumentData d = vis.data.data.get(i);
            if (d.size()-fromEnd<0) continue;
            double k = vis.current().bar(index).getClosePrice()/d.bar(d.size()-fromEnd).getClosePrice();
            TimeSeries xy = new TimeSeries(d.instrument.toString());
            int from = d.size()-fromEnd-14*24*60;
            int to = d.size()-fromEnd+14*24*60;
            for (int j = from;j<to;j+=10){
                if (j>=0 && j<d.size()){
                    xy.addOrUpdate(new Minute(Date.from(Instant.ofEpochSecond(d.bar(j).getEndTime().toEpochSecond()))),d.bar(j).getClosePrice()*k);
                }
            }
            all.addSeries(xy);
        }
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Market prices", // title
                "Time", // x-axis label
                "Price", // y-axis label
                all // data
        );



        panel = new ChartPanel(chart);
        panel.addChartMouseListener(this);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        frame.getContentPane().removeAll();
        frame.getContentPane().add(panel);
        frame.getContentPane().repaint();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setBounds(100,80,screen.width-100,screen.height-80);
        frame.setVisible(true);

    }

    @Override
    public void chartMouseClicked(ChartMouseEvent event) {
        MouseEvent m = event.getTrigger();
        ChartEntity e = event.getEntity();
        if (e instanceof XYItemEntity){
            TimeSeriesCollection dataset = (TimeSeriesCollection)((XYItemEntity) e).getDataset();
            TimeSeries ss = dataset.getSeries(((XYItemEntity) e).getSeriesIndex());
            String name = (String) ss.getKey();
            long time  = ss.getDataItem(((XYItemEntity) e).getItem()).getPeriod().getStart().toInstant().toEpochMilli()/1000;
            int barIndex = vis.current().getBarIndex(time);
            ZonedDateTime bartime = vis.current().bar(barIndex).getBeginTime();
            set(vis, barIndex);
            vis.setCurrent(name,bartime);
        }
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent chartMouseEvent) {

    }
}
