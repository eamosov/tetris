package ru.gustos.trading.visual;

import ru.gustos.trading.global.DecisionManager;
import ru.gustos.trading.global.PLHistory;
import ru.gustos.trading.utils.Interval;

import java.awt.*;
import java.util.ArrayList;
import java.util.function.IntUnaryOperator;

public class PaintUtils {

    public static void paintIntervals(Graphics g, Visualizator vis, Component comp, int from, int to, IntUnaryOperator index2x, boolean withProfits){
        paintTrades(g,vis,comp,from,to,index2x, withProfits);
        paintTrainIntervals(g,vis,comp,from,to,index2x, withProfits);
    }

    public static void paintTrades(Graphics g, Visualizator vis, Component comp, int from, int to, IntUnaryOperator index2x, boolean withProfits){
        if (vis.history!=null) {
            long fromTime = vis.current().bar(from).getEndTime().toEpochSecond();
            long toTime = vis.current().bar(Math.min(to-1,vis.current().size()-1)).getEndTime().toEpochSecond();
            for (PLHistory.PLTrade trade : vis.history.profitHistory) {
                if (trade.timeSell < fromTime) continue;
                if (trade.timeBuy > toTime) continue;
                int fromInd = vis.current().getBarIndex(trade.timeBuy);
                int toInd = vis.current().getBarIndex(trade.timeSell);
                int fromX = index2x.applyAsInt(fromInd);
                int toX = index2x.applyAsInt(toInd);
                int alpha = Math.abs(trade.profit - 1) > DecisionManager.limit(vis.current().instrument.component1()) ? 72 : 42;
                Color color = trade.profit > 1 ? CandlesPane.GREEN : CandlesPane.RED;
                g.setColor(VisUtils.alpha(color, alpha));
                g.fillRect(fromX, 0, toX - fromX + 1, comp.getHeight());
                if (withProfits) {
                    g.setColor(CandlesPane.darkerColor);
                    g.drawString(String.format("%.2g%%", (trade.profit - 1) * 100), fromX, g.getFont().getSize() * 3);
                }
            }
        }

    }

    public static void paintTrainIntervals(Graphics g, Visualizator vis, Component comp, int from, int to, IntUnaryOperator index2x, boolean withProfits){
        if (vis.trainIntervals!=null) {
            for (Interval ii : vis.trainIntervals) {
                if (ii.start > to) continue;
                if (ii.end < from) continue;
                int fromX = index2x.applyAsInt(ii.start);
                int toX = index2x.applyAsInt(ii.end);
                int alpha = 72;
                Color color = CandlesPane.BLUE;
                g.setColor(VisUtils.alpha(color, alpha));
                g.fillRect(fromX, 0, toX - fromX + 1, comp.getHeight());
            }
        }

    }

}
