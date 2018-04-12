package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.bot.OrderSideExt;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.OrderSide;
import ru.efreet.trading.logic.BotLogic;
import ru.efreet.trading.logic.impl.sd3.Sd3Logic;
import ru.gustos.trading.book.Sheet;

import java.awt.Color;
import java.util.stream.Collectors;

public class EfreetIndicator extends BaseIndicator  {
    public static int Id;

    public EfreetIndicator(IndicatorInitData data){
        super(data);
        Id = data.id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return "efreet";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public Color getColorMax() {
        return Color.green;
    }

    @Override
    public Color getColorMin() {
        return Color.red;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {

        final BotLogic logic = new Sd3Logic("sd3", Instrument.Companion.getBTC_USDT(),
                                            BarInterval.ONE_MIN, sheet.moments.stream()
                                                                              .map(m -> new XExtBar(m.bar))
                                                                              .collect(Collectors.toList()));
        logic.prepare();

        boolean buy = false;
        double buyWhenPrice = 0;
        double sellWhenPrice = 0;
        for (int i = 0; i < values.length; i++) {

            final OrderSideExt ose = logic.getAdvice(i, null, null, false).getOrderSide();

            XBar bar = sheet.moments.get(i).bar;
            if (ose.getSide() == OrderSide.BUY ){
                double price = bar.getMaxPrice();
                if (price>buyWhenPrice || bar.getClosePrice()>bar.getOpenPrice()*1.002) {
                    buy = true;
                    buyWhenPrice = 0;
                } else
                    buyWhenPrice = Math.min(buyWhenPrice,price*1.002);

                sellWhenPrice = 0;
            } else {
                double price = bar.getMinPrice();
                if (price<sellWhenPrice || bar.getClosePrice()*1.002<bar.getOpenPrice()) {
                    buy = false;
                    sellWhenPrice = 10000000;
                } else
                    sellWhenPrice = Math.max(sellWhenPrice,price/1.002);

                buyWhenPrice = 10000000;
            }
            if (buy)
                values[i] =IIndicator.YES;
            else
                values[i] =IIndicator.NO;
        }

    }
}

