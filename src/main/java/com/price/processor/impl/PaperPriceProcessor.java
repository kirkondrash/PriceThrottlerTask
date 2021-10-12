package com.price.processor.impl;

import com.price.processor.PriceProcessor;

public class PaperPriceProcessor implements PriceProcessor {

    @Override
    public void onPrice(String ccyPair, double rate) {
        try {
            Thread.sleep(6000);
            System.out.printf("Paper %s - %f%n", ccyPair, rate);
        } catch (InterruptedException e) {
            System.out.printf("Digital %s - interrupted", ccyPair);
        }
    }

    @Override
    public void subscribe(PriceProcessor priceProcessor) {
        throw new RuntimeException("Not intended!");
    }

    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {
        throw new RuntimeException("Not intended!");
    }
}
