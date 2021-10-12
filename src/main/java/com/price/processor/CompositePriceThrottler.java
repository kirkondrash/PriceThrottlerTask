package com.price.processor;

import com.price.processor.impl.DigitalPriceProcessor;
import com.price.processor.impl.PaperPriceProcessor;

import java.util.*;
import java.util.concurrent.*;

public class CompositePriceThrottler implements PriceProcessor {

    private Map<String, CurrencyPairPriceProcessor> currencyPairPriceThrottlers;

    public CompositePriceThrottler() {
        this.currencyPairPriceThrottlers = new ConcurrentHashMap<>();
    }

    /* We don't create the throttlers for each price beforehand, but upon receiving first tick.
    * Throttler per price pair was designed to combat the problem of rapid changing price pairs
    * taking up full queue/thread pool. Alternative solution is build smth like Priority Queue here.
    * But still we will always choose between how fast the changes are delivered vs. resources we spend.
     */
    @Override
    public void onPrice(String ccyPair, double rate) {
        currencyPairPriceThrottlers
                .computeIfAbsent(ccyPair, CurrencyPairPriceThrottler::new)
                .onPrice(ccyPair, rate);
    }

    @Override
    public void subscribe(PriceProcessor priceProcessor) {
        currencyPairPriceThrottlers.values().forEach(currencyPriceProcessor -> currencyPriceProcessor.subscribe(priceProcessor));
    }

    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {
        currencyPairPriceThrottlers.values().forEach(currencyPriceProcessor -> currencyPriceProcessor.unsubscribe(priceProcessor));
    }

    public static void main(String[] args) {
        CompositePriceThrottler compositePriceThrottler = new CompositePriceThrottler();

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            compositePriceThrottler.onPrice("QUICKPAIR", Math.random());
        }, 0, 1500, TimeUnit.MILLISECONDS);

        scheduledExecutorService.scheduleAtFixedRate(()->{
            compositePriceThrottler.onPrice("SLOWPAIR", Math.random());
        },0,5000, TimeUnit.MILLISECONDS);


        compositePriceThrottler.subscribe(new DigitalPriceProcessor());
        compositePriceThrottler.subscribe(new PaperPriceProcessor());
    }
}
