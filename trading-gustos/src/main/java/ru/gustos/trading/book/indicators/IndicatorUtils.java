package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.util.ArrayList;


public class IndicatorUtils{

    public static int bars(IndicatorPeriod period,Sheet sheet){
        switch (period){
            case TENMINUTES:
                return 2;
            case HOUR:
                return 4;
            case DAY:
                return 6;
            case WEEK:
                return 8;
            case MONTH:
                return 10;
        }
//        switch (period){
//            case TENMINUTES:
//                return 601/(int)sheet.interval().getDuration().getSeconds();
//            case HOUR:
//                return 3601/(int)sheet.interval().getDuration().getSeconds();
//            case DAY:
//                return (3600*24+1)/(int)sheet.interval().getDuration().getSeconds();
//            case WEEK:
//                return (3600*24*7+1)/(int)sheet.interval().getDuration().getSeconds();
//            case MONTH:
//                return (3600*24*30+1)/(int)sheet.interval().getDuration().getSeconds();
//        }
        throw new NullPointerException();
    }


}
