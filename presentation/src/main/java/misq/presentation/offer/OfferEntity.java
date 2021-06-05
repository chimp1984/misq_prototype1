/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package misq.presentation.offer;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import misq.common.data.Couple;
import misq.finance.swap.offer.SwapOffer;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

public class OfferEntity implements Comparable<OfferEntity> {
    protected Disposable marketPriceDisposable;
    protected final SwapOffer offer;
    protected final BehaviorSubject<Double> marketPriceSubject;
    protected double price;
    protected final String formattedBaseAmountWithMinAmount;
    protected final String formattedTransferOptions;
    protected String formattedPrice;
    protected String formattedQuoteAmount;

    public OfferEntity(SwapOffer offer, BehaviorSubject<Double> marketPriceSubject) {
        this.offer = offer;
        this.marketPriceSubject = marketPriceSubject;

        formattedBaseAmountWithMinAmount = OfferFormatter.formatAmountWithMinAmount(offer.getBaseAsset().getAmount(),
                offer.getMinAmountAsPercentage(),
                offer.getBaseCurrency());
        formattedTransferOptions = OfferFormatter.formatTransferOptions(offer.getTransferOptions());

        marketPriceDisposable = marketPriceSubject.subscribe(this::updatedPriceAndAmount, Throwable::printStackTrace);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void activate() {
        marketPriceDisposable = marketPriceSubject.subscribe(this::updatedPriceAndAmount);
    }

    public void deactivate() {
        marketPriceDisposable.dispose();
    }

    public double getPrice() {
        return price;
    }

    public SwapOffer getOffer() {
        return offer;
    }

    public BehaviorSubject<Double> getMarketPriceSubject() {
        return marketPriceSubject;
    }

    public String getFormattedBaseAmountWithMinAmount() {
        return formattedBaseAmountWithMinAmount;
    }

    public String getFormattedTransferOptions() {
        return formattedTransferOptions;
    }

    public String getFormattedPrice() {
        return formattedPrice;
    }

    public String getFormattedQuoteAmount() {
        return formattedQuoteAmount;
    }

    public int compareBaseAmount(OfferEntity other) {
        return Long.compare(offer.getBaseAsset().getAmount(), other.getOffer().getBaseAsset().getAmount());
    }

    public int compareQuoteAmount(OfferEntity other) {
        return Long.compare(offer.getQuoteAsset().getAmount(), other.getOffer().getQuoteAsset().getAmount());
    }

    public int comparePrice(OfferEntity other) {
        return Double.compare(price, other.price);
    }

    @Override
    public int compareTo(@NotNull OfferEntity other) {
        return comparePrice(other);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Internal
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected void updatedPriceAndAmount(double marketPrice) {
        Couple<String, Double> priceTuple = getPriceTuple(offer.getFixPrice(),
                offer.getMarketBasedPrice(),
                marketPrice);
        formattedPrice = priceTuple.first;
        price = priceTuple.second;
        formattedQuoteAmount = getFormattedQuoteAmount(offer.getBaseAsset().getAmount(),
                offer.getMinAmountAsPercentage(),
                offer.getMarketBasedPrice(),
                marketPrice,
                offer.getQuoteAsset().getCode());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Internal
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    // Todo: Just preliminary, might get moved out to a util class in that package
    private static Couple<String, Double> getPriceTuple(double fixPrice, Optional<Double> marketBasedPrice, double marketPrice) {
        double percentage;
        double price;
        DecimalFormat df = new DecimalFormat("#.##");
        df.setMinimumFractionDigits(2);
        if (marketBasedPrice.isPresent()) {
            percentage = marketBasedPrice.get();
            price = marketPrice * (1 + percentage);
        } else {
            percentage = marketPrice / fixPrice;
            price = fixPrice;
        }
        String displayString = df.format(price) + " (" + df.format(percentage * 100) + "%)";
        return new Couple<>(displayString, price);
    }

    private static String getFormattedQuoteAmount(long baseAmount, Optional<Double> minAmountAsPercentage,
                                                  Optional<Double> marketBasedPrice, double marketPrice, String currencyCode) {
        double amount = getQuoteAmount(baseAmount, marketBasedPrice, marketPrice) / 10000d;
        String minAmountString;
        DecimalFormat df = new DecimalFormat("#.##");
        df.setMinimumFractionDigits(2);
        if (minAmountAsPercentage.isPresent()) {
            long minAmount = Math.round(amount * minAmountAsPercentage.get());
            minAmountString = df.format(minAmount) + " - ";
        } else {
            minAmountString = "";
        }
        return minAmountString + df.format(amount) + " " + currencyCode;
    }

    private static long getMinQuoteAmount(long baseAmount, Optional<Double> minAmountAsPercentage, Optional<Double> marketBasedPrice, double marketPrice) {
        double amount;
        double percentage;
        double price;

        if (marketBasedPrice.isPresent()) {
            percentage = marketBasedPrice.get();
            price = marketPrice * (1 + percentage);
            amount = baseAmount / 100000000d * price;
        } else {
            amount = baseAmount / 100000000d;
        }

        if (minAmountAsPercentage.isPresent()) {
            long minAmount = Math.round(amount * minAmountAsPercentage.get());
            return Math.round(minAmount * 10000);
        } else {
            return Math.round(amount * 10000);
        }
    }

    private static long getQuoteAmount(long baseAmount, Optional<Double> marketBasedPrice, double marketPrice) {
        double amount;
        double percentage;
        double price;

        if (marketBasedPrice.isPresent()) {
            percentage = marketBasedPrice.get();
            price = marketPrice * (1 + percentage);
            amount = baseAmount / 100000000d * price;
        } else {
            amount = baseAmount / 100000000d;
        }
        return Math.round(amount * 10000);
    }

    private static double getPrice(double fixPrice, Optional<Double> marketBasedPrice, Optional<Double> marketPrice) {
        return marketBasedPrice.map(percentage -> marketPrice
                .map(marketPrice1 -> marketPrice1 * (1 + percentage)).orElse(fixPrice))
                .orElse(fixPrice);
    }

    private static long getMinQuoteAmountValue(List<OfferEntity> offers, Double marketPrice) {
        return offers.stream()
                .mapToLong(o -> getMinQuoteAmount(o.getOffer().getBaseAsset().getAmount(),
                        o.getOffer().getMinAmountAsPercentage(),
                        o.getOffer().getMarketBasedPrice(),
                        marketPrice))
                .min()
                .orElse(0);
    }

    private static long getMaxQuoteAmountValue(List<OfferEntity> offers, Double marketPrice) {
        return offers.stream()
                .mapToLong(o -> getQuoteAmount(o.getOffer().getBaseAsset().getAmount(),
                        o.getOffer().getMarketBasedPrice(),
                        marketPrice))
                .max()
                .orElse(0);
    }

    private static double getLowestPrice(List<SwapOffer> offers, Optional<Double> marketPrice) {
        return offers.stream()
                .mapToDouble(offer -> getPrice(offer.getFixPrice(), offer.getMarketBasedPrice(), marketPrice))
                .min()
                .orElse(0);
    }

    private static double getHighestPrice(List<SwapOffer> offers, Optional<Double> marketPrice) {
        return offers.stream()
                .mapToDouble(offer -> getPrice(offer.getFixPrice(), offer.getMarketBasedPrice(), marketPrice))
                .max()
                .orElse(0);
    }

    private static int comparePrice(Double selfPrice, Double otherPrice, String bidAssetCode, String quoteCurrencyCode) {
        if (bidAssetCode.equals(quoteCurrencyCode)) {
            return Double.compare(otherPrice, selfPrice);
        } else {
            return Double.compare(selfPrice, otherPrice);
        }
    }

}
