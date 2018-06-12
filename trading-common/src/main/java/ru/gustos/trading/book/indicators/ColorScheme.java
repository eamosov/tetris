package ru.gustos.trading.book.indicators;

import java.awt.*;

public class ColorScheme{
    public static ColorScheme REDGREEN = new ColorScheme(Color.RED, Color.GREEN);
    public static ColorScheme GREENGRAY = new ColorScheme(Color.GREEN, Color.GRAY);
    public static ColorScheme REDGRAY = new ColorScheme(Color.RED, Color.GRAY);
    public static ColorScheme WHITEBLUE = new ColorScheme(Color.lightGray, Color.BLUE);

    Color min;
    Color max;

    public ColorScheme(Color min, Color max){
        this.min = min;
        this.max = max;
    }

    public Color lineColor(int line) {
        return line==0?max:min;
    }

    public Color min(){ return min;}
    public Color max(){ return max;}

    public float stroke(int line) {
        return line==0?2.0f:1.0f;
    }
}
