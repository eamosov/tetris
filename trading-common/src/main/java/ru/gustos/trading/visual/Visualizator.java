package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.efreet.trading.bot.TradeHistory;
import ru.gustos.trading.book.PlayIndicator;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.indicators.IIndicator;
import ru.gustos.trading.book.indicators.IndicatorType;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;

public class Visualizator {
    private Sheet sheet;
    private int from;
    private int zoom;
    private VisualizatorFrame frame;
    protected EventListenerList viewListeners = new EventListenerList();

    int backIndicator = -1;
    int graphIndicator = -1;
    private ArrayList<Integer> selectedIndicators = new ArrayList<>();
    PlayIndicator.PlayResults playResult = null;

    double param = 0;
    int averageWindow = 50;
    String averageType = "None";

    public Visualizator(Sheet sheet){
        this.sheet = sheet;
        frame = new VisualizatorFrame(this);
        frame.setVisible(true);
        setIndex(1000000);
    }

    public void addListener(VisualizatorViewListener listener){
        viewListeners.add(VisualizatorViewListener.class,listener);
    }

    public void addListener(VisualizatorMouseListener listener){
        viewListeners.add(VisualizatorMouseListener.class,listener);
    }

    public void fireViewUpdated(){
        Arrays.stream(viewListeners.getListeners(VisualizatorViewListener.class)).forEach(VisualizatorViewListener::visualizatorViewChanged);
    }

    public void setFrom(ZonedDateTime from){
        this.from = sheet.getBarIndex(from);
        fireViewUpdated();
    }

    public void goLeft() {
        from-=barsOnScreen();
        fixFrom();
        fireViewUpdated();
    }

    public void goRight() {
        from+=barsOnScreen();
        fixFrom();
        fireViewUpdated();
    }

    public int getIndex() {
        return from;
    }

    public int getEndIndex() {
        return from+barsOnScreen()-1;
    }

    public void setIndex(int from){
        this.from = from;
        fixFrom();
        frame.form.setInfo(from,null);
        fireViewUpdated();
    }

    public void setMiddleIndex(int index){
        setIndex(index-barsOnScreen()/2);
    }

    public int barsOnScreen(){
        return frame.form.getCenter().getWidth()*zoomScale()/candleWidth();
    }

    public int zoomScale(){
        return 1<<zoom;
    }

    private void fixFrom(){
        if (from<0) from = 0;
        from = from/zoomScale()*zoomScale();
        if (from>sheet.size()-barsOnScreen())
            from = sheet.size()-barsOnScreen();
    }

    public int candleWidth(){
        return 9;
    }

    public Sheet getSheet() {
        return sheet;
    }

    public void zoomPlus() {
        if (zoom>=9) return;
        zoom++;
        frame.form.setZoom(zoom);
        fixFrom();
        fireViewUpdated();
    }

    public void zoomMinus() {
        if (zoom<=0) return;
        zoom--;
        frame.form.setZoom(zoom);
        fixFrom();
        fireViewUpdated();
    }

    public int getIndexAt(Point point) {
        return getIndex()+point.x*zoomScale()/candleWidth();
    }

    public void mouseMove(Point point) {
        startDrag = null;
        Arrays.stream(viewListeners.getListeners(VisualizatorMouseListener.class)).forEach(l -> l.visualizatorMouseMoved(point));
    }

    private Point startDrag;
    private int startDragIndex;
    public void mousePressed(Point point) {
        startDrag = point;
        startDragIndex = from;
    }

    public void mouseReleased(Point point) {
        startDrag = null;
    }
    public void mouseExited(Point point) {
        startDrag = null;
    }
    public void mouseDrag(Point point) {
        if (startDrag!=null){
            int dif = getIndexAt(point)-getIndexAt(startDrag);
            setIndex(startDragIndex-dif);
        }
    }

    public void mouseClicked(Point point) {
        Arrays.stream(viewListeners.getListeners(VisualizatorMouseListener.class)).forEach(l -> l.visualizatorMouseClicked(point));
    }

    public static void main(String[] args) {
        try {
            Sheet sheet = new Sheet();
            sheet.fromCache(500);
//            sheet.fromExchange();
            SheetUtils.FillDecisions(sheet);
            sheet.calcIndicators();
            new Visualizator(sheet);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public void goLeftToIndicator() {
        int ind = from+barsOnScreen()/2-zoomScale();
        double val = sheet.getData().get(backIndicator,ind);
        while (ind>0 && sheet.getData().get(backIndicator,ind)==val)
            ind--;
        setIndex(ind-barsOnScreen()/2);
    }

    public void goRightToIndicator() {
        int ind = from+barsOnScreen()/2+zoomScale();
        double val = sheet.getData().get(backIndicator,ind);
        while (ind<sheet.size() && sheet.getData().get(backIndicator,ind)==val)
            ind++;
        setIndex(ind-barsOnScreen()/2);
    }

    public void updateSelectedIndicator(int ind) {
        IIndicator ii = sheet.getLib().get(ind);
        selectedIndicators.remove((Object)ind);
        selectedIndicators.removeIf(c->sheet.getLib().get(c).getType()==ii.getType());
        selectedIndicators.add(ind);
        backIndicator = -1;
        graphIndicator = -1;
        playResult = null;
        for (int i = 0;i<selectedIndicators.size();i++) {
            Integer id = selectedIndicators.get(i);
            IndicatorType type = sheet.getLib().get(id).getType();
            if (type == IndicatorType.YESNO)
                backIndicator = id;
            else
                graphIndicator = id;
        }
        frame.form.updateScrollToIndicators();
        fireViewUpdated();
    }

    public void setParam(double v) {
        param = v;
        fireViewUpdated();
    }

    public void runPlay() {
        if (backIndicator!=-1){
            playResult = new PlayIndicator().playIndicator(sheet, backIndicator,from,sheet.size()-1);
            JOptionPane.showMessageDialog(null, playResult.toString());
            fireViewUpdated();
        }
    }

    public void setPlayHistory(TradeHistory history){
        playResult = new PlayIndicator.PlayResults(sheet, history);
        fireViewUpdated();
    }

    public void setAverage(String type, int window) {
        averageWindow = window;
        averageType = type;
        fireViewUpdated();
    }
}

