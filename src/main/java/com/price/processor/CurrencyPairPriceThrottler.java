package com.price.processor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

public class CurrencyPairPriceThrottler implements CurrencyPairPriceProcessor {

    private String ccyPair;
    volatile private PriceUpdate currentPrice;
    volatile private PriceUpdate updatePrice;
    private Map<PriceProcessor, ScheduledFuture<?>> subscribers;
    private ScheduledExecutorService scheduledExecutorService;

    public CurrencyPairPriceThrottler(String ccyPair) {
        this.ccyPair = ccyPair;
        this.scheduledExecutorService = Executors.newScheduledThreadPool(5);
        this.subscribers = new LinkedHashMap<>();
        ((ScheduledThreadPoolExecutor) this.scheduledExecutorService).setRemoveOnCancelPolicy(true);
    }

    // We shouldn't start updating the prices as soon as we receive an update -
    // we have to throttle them, and lessen the load on consumers by sending only updated currency pairs.
    @Override
    public void onPrice(String ccyPair, double rate) {
        if (!this.ccyPair.equals(ccyPair)) {
            throw new RuntimeException(String.format("This is a wrong PriceProcessor! Intended ccyPair %s, actual %s", this.ccyPair, ccyPair));
        }
        updatePrice = new PriceUpdate(rate);
    }

    // While here it is hardcoded, we can parametrize the throttling - make it skip more or less pairs from rapid source.
    @Override
    public void subscribe(final PriceProcessor priceProcessor) {
        ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (updatePrice.equals(currentPrice)) {
                return;
            }
            currentPrice = updatePrice;
            priceProcessor.onPrice(ccyPair+" at "+currentPrice.getTimestamp(), currentPrice.getRate());

        }, 1, 1, TimeUnit.SECONDS);
        subscribers.put(priceProcessor, scheduledFuture);
    }

    @Override
    public void unsubscribe(PriceProcessor priceProcessor) {
        ScheduledFuture<?> scheduledFuture = subscribers.remove(priceProcessor);
        // Whether we should cancel a running price update or not is arguable, but such a possibility exists.
        // Of course, this means that the subscriber in his method should handle InterruptedException and free resources.
        scheduledFuture.cancel(true);
    }

    // Utility class for inner use, so not a distinct file
    class PriceUpdate {
        private double rate;
        private long timestamp;

        public PriceUpdate(double rate) {
            this.rate = rate;
            this.timestamp = Instant.now().getEpochSecond();
        }

        public double getRate() {
            return rate;
        }

        public double getTimestamp() {
            return rate;
        }

        // We cannot compare price changes by double values - they are not exact. A timestamp will do.
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PriceUpdate that = (PriceUpdate) o;
            return timestamp == that.timestamp;
        }

        @Override
        public int hashCode() {
            return Objects.hash(timestamp);
        }
    }
}
