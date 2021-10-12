package com.price.processor;

/* Only to emphasize interface aim from the PriceProcessor - that we have processors dedicated per price pair.
* We could decide to make some foolproof precautions like make user implement methods to set/get currency pair,
* But I decided it was out of scope here.
 */
public interface CurrencyPairPriceProcessor extends PriceProcessor {
}

