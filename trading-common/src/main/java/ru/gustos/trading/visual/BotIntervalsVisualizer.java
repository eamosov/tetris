package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.bots.BotInterval;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class BotIntervalsVisualizer extends JFrame {

    public BotIntervalsVisualizer(Sheet sheet, ArrayList<BotInterval> intervals){
        super("Bot intervals");
        int columns = 8;
        getContentPane().setLayout(new GridLayout((intervals.size()-1)/columns+1,columns,4,4));
        for (int i = 0;i<intervals.size();i++){
            getContentPane().add(new BotIntervalPanel(sheet,intervals.get(i)));
        }
        setSize(1700,800);
        setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
}

class BotIntervalPanel extends JPanel {
    Sheet sheet;
    BotInterval interval;
    BotIntervalPanel(Sheet sheet, BotInterval interval){
        this.sheet = sheet;
        this.interval = interval;
    }

    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(Color.white);
        g.fillRect(0,0,getWidth(),getHeight());
        g.setColor(CandlesPane.darkColor);
        g.drawRect(0,0,getWidth(),getHeight());
        int w = getWidth();
        double min = Double.MAX_VALUE, max = 0;
        int buy = interval.buyMoment;
        int sell = interval.sellMoment;
        int fromIndex = buy-(sell-buy)/4;
        int toIndex = sell+(sell-buy)/4;
        for (int i = 0; i < w; i++) {
            int from = fromIndex + (toIndex - fromIndex) * i / w;
            int to = fromIndex + (toIndex - fromIndex) * (i + 1) / w;
            if (to == from) to = from + 1;

            Pair<Double, Double> minMax = SheetUtils.getMinMax(sheet, from, to);
            if (minMax.getFirst() < min) min = minMax.getFirst();
            if (minMax.getSecond() > max) max = minMax.getSecond();

        }
        for (int i = 0; i < w; i++) {
            int from = fromIndex + (toIndex - fromIndex) * i / w;
            int to = fromIndex + (toIndex - fromIndex) * (i + 1) / w;
            if (to == from) to = from + 1;

            if (buy>=from && buy<to){
                g.setColor(CandlesPane.BLUE);
                g.drawLine(i,0,i,getHeight());
            }
            if (sell>=from && sell<to){
                g.setColor(CandlesPane.BLUE);
                g.drawLine(i,0,i,getHeight());
            }


            Pair<Double, Double> minMax = SheetUtils.getMinMax(sheet, from, to);


            int hmin = (int)((minMax.getFirst()-min)/(max-min)*getHeight());
            int hmax = (int)((minMax.getSecond()-min)/(max-min)*getHeight());
            g.setColor(CandlesPane.darkerColor);
            g.drawLine(i,getHeight()-hmin,i,getHeight()-hmax);


        }
        g.setFont(CandlesPane.gridFont);
        double profit = sheet.moments.get(sell).bar.getClosePrice() / sheet.moments.get(buy).bar.getClosePrice();
        g.setColor(profit>1?CandlesPane.GREEN:CandlesPane.RED);
        g.drawString(String.format("%.3f", profit),0,getHeight());
    }
}