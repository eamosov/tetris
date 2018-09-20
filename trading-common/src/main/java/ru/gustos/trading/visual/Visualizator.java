package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.efreet.trading.bot.TradeHistory;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.book.Extrapolation;
import ru.gustos.trading.book.PlayIndicator;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.book.indicators.Indicator;
import ru.gustos.trading.book.indicators.IndicatorsData;
import ru.gustos.trading.global.*;
import ru.gustos.trading.utils.Interval;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;

public class Visualizator {
    ExperimentData data;

    private int from;
    private int vzoom;
    public int zoom;
    public VisualizatorFrame frame;
    protected EventListenerList viewListeners = new EventListenerList();
    private IndicatorsFrame indicatorsFrame;

    private Instrument current;
    double param = 0;
    int averageWindow = 50;
    String averageType = "None";
    private boolean fixedVolumes = false;
    private boolean gustosVolumes = false;
    private int selectedIndex;
    private boolean fullZoom = false;
    private double lineAtPrice = 0;
    private double selectedPrice1 = 0;
    private double selectedPrice2 = 0;
    PLHistory history = null;
    int selectedHistory;

    boolean showMinMax;

    Extrapolation extrapolation = null;
    BoundlinesFinder levels = null;
    ArrayList<Interval> trainIntervals = null;

    public Visualizator(ExperimentData data){
        this.data = data;
        current = data.data.get(0).instrument;
        frame = new VisualizatorFrame(this);
        frame.setVisible(true);
        setIndex(1000000);
    }

    public void addListener(VisualizatorViewListener listener){
        viewListeners.add(VisualizatorViewListener.class,listener);
    }

    public void addListener(VisualizatorBarAtMouseListener listener){
        viewListeners.add(VisualizatorBarAtMouseListener.class,listener);
    }

    public void addListener(VisualizatorMouseListener listener){
        viewListeners.add(VisualizatorMouseListener.class,listener);
    }

    public void fireViewUpdated(){
        Arrays.stream(viewListeners.getListeners(VisualizatorViewListener.class)).forEach(VisualizatorViewListener::visualizatorViewChanged);
    }

    public InstrumentData current(){
        return data.getInstrument(current.toString());
    }

    public void setFrom(ZonedDateTime from){
        this.from = current().getBarIndex(from);
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
        return frame.form.getCandlesPane().getWidth()*zoomScale()/candleWidth();
    }

    public int zoomScale(){
        return 1<<zoom;
    }

    private void fixFrom(){
        if (from<0) from = 0;
        from = from/zoomScale()*zoomScale();
        if (from>current().size()-barsOnScreen())
            from = current().size()-barsOnScreen();
    }

    public int candleWidth(){
        return 9;
    }

    public void zoomPlus() {
        zoomPlus(from+barsOnScreen()/2);
    }
    public void zoomPlus(int index) {
        if (zoom>=12) return;
        double pos = (index-from)*1.0/barsOnScreen();
        zoom++;
        if (barsOnScreen()>current().size()) {
            zoom--;
            return;
        }
        from = index-(int)(barsOnScreen()*pos);
        frame.form.setZoom(zoom);
        fixFrom();
        fireViewUpdated();
    }

    public void zoomMinus() {
        zoomMinus(from+barsOnScreen()/2);
    }
    public void zoomMinus(int index) {
        if (zoom<=0) return;
        double pos = (index-from)*1.0/barsOnScreen();
        zoom--;
        from = index-(int)(barsOnScreen()*pos);
        frame.form.setZoom(zoom);
        fixFrom();
        fireViewUpdated();
    }

    public int getIndexAt(Point point) {
        return getIndex()+point.x/candleWidth()*zoomScale();
    }

    public void mouseMove(Point point) {
        startDrag = null;
        Arrays.stream(viewListeners.getListeners(VisualizatorMouseListener.class)).forEach(l -> l.visualizatorMouseMoved(point));
    }

    private Point startDrag;
    private int startDragIndex;
    private double startDragPrice;
    public void mousePressed(Point point, int button) {
        startDrag = point;
        startDragIndex = from;
        startDragPrice = frame.form.getCandlesPane().screen2price(point.y);
        Arrays.stream(viewListeners.getListeners(VisualizatorMouseListener.class)).forEach(l -> l.visualizatorMousePressed(point, button));
    }

    public void mouseReleased(Point point, int button) {
        startDrag = null;
        Arrays.stream(viewListeners.getListeners(VisualizatorMouseListener.class)).forEach(l -> l.visualizatorMousePressed(point, button));

    }
    public void mouseExited(Point point) {
        startDrag = null;
    }
    public void mouseDrag(Point point, int button, int modifiers) {
        if ((modifiers & Event.SHIFT_MASK)!=0){
            setSelectedPrice(startDragPrice,frame.form.getCandlesPane().screen2price(point.y));
        } else if (startDrag!=null){
            int dif = getIndexAt(point)-getIndexAt(startDrag);
            setIndex(startDragIndex-dif);
        }
    }

    public void mouseClicked(Point point, int button) {
        Arrays.stream(viewListeners.getListeners(VisualizatorMouseListener.class)).forEach(l -> l.visualizatorMouseClicked(point, button));
    }

    public void moveToIndicator(int dir) {
//        ArrayList<Indicator> back = sheet.getLib().indicatorsBack;
//        if (back.size()==0) return;
//        int start = from+barsOnScreen()/2+zoomScale()*dir;
//        int ind = start;
//        boolean found = false;
//        IndicatorsData data = sheet.getData();
//        while (ind>0 && ind<sheet.size() && !found) {
//            ind+=dir;
//            for (Indicator i : back){
//                if (data.get(i,start)!= data.get(i,ind)) {
//                    found = true;
//                    break;
//                }
//            }
//        }
//        setIndex(ind-barsOnScreen()/2);
    }

    public void updateSelectedIndicator(int ind) {
//        Indicator indicator = sheet.getLib().get(ind);
//        setIndicatorShow(indicator,!indicator.data.show);
    }

    public void setParam(double v) {
        param = v;
        fireViewUpdated();
    }

    public void runPlay() {
//        ArrayList<Indicator> back = sheet.getLib().indicatorsBack;
//        if (back.size()==1){
//            playResult = new PlayIndicator().playIndicator(sheet, back.get(0).getId(),from,sheet.size()-1);
//            JOptionPane.showMessageDialog(null, playResult.toString());
//            fireViewUpdated();
//        } else
//            JOptionPane.showMessageDialog(null, String.format("back indicators: %d, must be 1", back.size()));
    }

    public void setPlayHistory(TradeHistory history){
//        playResult = new PlayIndicator.PlayResults(sheet, history);
//        fireViewUpdated();
    }

    public void setAverage(String type, int window) {
        averageWindow = window;
        averageType = type;
        fireViewUpdated();
    }

    public void onShowIndicators() {
        if (indicatorsFrame==null){
            indicatorsFrame = new IndicatorsFrame(this);
        } else {
            if (!indicatorsFrame.isVisible())
                indicatorsFrame.setVisible(true);
            else if (!indicatorsFrame.isFocused())
                indicatorsFrame.requestFocus();
            else
                indicatorsFrame.setVisible(false);

        }
    }

    public void setIndicatorShowOnBottom(Indicator indicator, boolean show) {
        indicator.data.showOnBottom = show;
        indicatorUpdated();
    }

    public void setIndicatorShow(Indicator indicator, boolean show) {
        indicator.data.show = show;
        indicatorUpdated();
    }

    private void indicatorUpdated(){
//        sheet.getLib().sortIndicators();
        fireViewUpdated();
        frame.form.getCenter().revalidate();
        frame.form.getCenter().repaint();
    }

    public void setVerticalZoom(int zoom) {
        vzoom = zoom;
        fireViewUpdated();
    }

    public int getVZoom() {
        return vzoom;
    }

    public void setFixedVolumes(boolean state) {
        fixedVolumes = state;
        fireViewUpdated();
    }

    public void setGustosVolumes(boolean state) {
        gustosVolumes = state;
        fireViewUpdated();
    }

    public boolean getFixedVolumes(){
        return fixedVolumes;
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
        Arrays.stream(viewListeners.getListeners(VisualizatorBarAtMouseListener.class)).forEach(i->i.visualizatorBarAtMouseChanged(selectedIndex));
    }

    public double getLineAtPrice(){
        return lineAtPrice;
    }

    public Pair<Double,Double> getSelectedPrice(){
        if (selectedPrice2==0) return null;
        return new Pair<>(selectedPrice1, selectedPrice2);
    }

    public void setLineAtPrice(double price) {
        lineAtPrice = price;
        selectedPrice1 = 0;
        selectedPrice2 = 0;
        fireViewUpdated();
    }

    private void setSelectedPrice(double price1, double price2) {
        lineAtPrice = 0;
        selectedPrice1 = Math.min(price1,price2);
        selectedPrice2 = Math.max(price1,price2);
        fireViewUpdated();
    }

    public int getSelectedIndex(){
        return selectedIndex;
    }

    public boolean getGustosVolumes() {
        return gustosVolumes;
    }

    public void right() {
        from+=zoomScale();
        fixFrom();
        setSelectedIndex(from+barsOnScreen()-1);
        fireViewUpdated();
    }

    public void left() {
        from-=zoomScale();
        fixFrom();
        setSelectedIndex(from+barsOnScreen()-1);
        fireViewUpdated();
    }

    public void setFullZoom(boolean state) {
        fullZoom = state;
        fireViewUpdated();
    }

    public boolean getFullZoom() {
        return fullZoom;
    }

    public void setShowMinMax(boolean state) {
        showMinMax = state;
        if (showMinMax)
            current().initMinMax();
        fireViewUpdated();
    }

    public boolean getShowMinMax() {
        return showMinMax;
    }

    public void setExtrapolation(Extrapolation e){
        extrapolation = e;
        fireViewUpdated();
    }

    public void setLevels(BoundlinesFinder l){
        levels = l;
        fireViewUpdated();
    }

    public void setCurrent(String instr) {
        setCurrent(instr,current().bar(from).getBeginTime());
    }
    public void setCurrent(String instr, ZonedDateTime time) {
        if (!instr.equals(current().instrument.toString())) {
            current = data.getInstrument(instr).instrument;
            trainIntervals = null;
            if (showMinMax)
                current().initMinMax();
            updateHistory();
            setIndex(current().getBarIndex(time));
        }
    }

    public void setShowTradeHistory(int selected) {
        selectedHistory = selected;
        updateHistory();
        fireViewUpdated();
    }

    private void updateHistory() {
        if (selectedHistory==0)
            history = null;
        else if (selectedHistory==1)
            history = data.planalyzer2.get(current.toString());
        else if (selectedHistory==2)
            history = data.planalyzer1.get(current.toString());
    }

    public void enableTrainZones(int index) {
        if (index==0){
            trainIntervals = null;
            return;
        }
        PLHistory h = data.planalyzer1.get(current.toString());
        ArrayList<PLHistory.CriticalMoment> moments = h.makeGoodBadMoments(DecisionManager.limit(current.component1()), current().bar(index).getEndTime().toEpochSecond(), 6, 12);
        trainIntervals = new ArrayList<>();
        for (PLHistory.CriticalMoment m : moments){
            int ii = current().bars.findIndex(m.timeBuy);
            trainIntervals.add(new Interval(ii-180,ii+180));
        }
        fireViewUpdated();
    }
}

