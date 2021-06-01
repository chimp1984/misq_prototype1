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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.common.util.Tuple2;
import misq.finance.offer.Offerbook;
import misq.finance.swap.offer.SwapOffer;
import misq.presentation.Model;
import misq.presentation.marketprice.MarketPriceService;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class OfferbookModel implements Model {
    private final Offerbook offerbook;
    private final MarketPriceService marketPriceService;

    // Exposed for filter model
    final ObservableList<OfferListItem> offerItems = FXCollections.observableArrayList();
    final Set<Predicate<OfferListItem>> listFilterPredicates = new CopyOnWriteArraySet<>();
    Predicate<OfferListItem> currencyPredicate = e -> true;
    String baseCurrency;

    private final FilteredList<OfferListItem> filteredItems = new FilteredList<>(offerItems);

    // exposed for view
    @Getter
    private final SortedList<OfferListItem> sortedItems = new SortedList<>(filteredItems);
    @Getter
    private final StringProperty selectedAskCurrency = new SimpleStringProperty();
    @Getter
    private final StringProperty selectedBidCurrency = new SimpleStringProperty();
    @Getter
    private final DoubleProperty marketPrice = new SimpleDoubleProperty();
    @Getter
    private final ObservableList<String> currencies = FXCollections.observableArrayList("BTC", "USD", "EUR", "XMR", "USDT");
    @Getter
    private final RangeFilterModel amountFilterModel;

    public OfferbookModel(Offerbook offerbook, MarketPriceService marketPriceService) {
        this.offerbook = offerbook;
        this.marketPriceService = marketPriceService;
        amountFilterModel = new RangeFilterModel(this);
    }

    @Override
    public void initialize() {
        selectedAskCurrency.set("BTC");
        selectedBidCurrency.set("USD");
        marketPrice.set(marketPriceService.getMarketPrice());
        offerItems.clear();
        offerItems.addAll(offerbook.getOffers().stream()
                .filter(e -> e instanceof SwapOffer)
                .map(e -> (SwapOffer) e)
                .map(this::toOfferListItem)
                .collect(Collectors.toList()));
        applyBaseCurrency();
        amountFilterModel.initialize();
    }

    @Override
    public void activate() {
        reset();
        Predicate<OfferListItem> predicate = item -> item.getOffer().getAskAsset().getCode().equals(selectedAskCurrency.get());
        setCurrencyPredicate(predicate);
        amountFilterModel.activate();
    }

    @Override
    public void deactivate() {
        amountFilterModel.deactivate();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void addOffer(SwapOffer offer) {
        offerItems.add(toOfferListItem(offer));
    }

    public void removeOffer(SwapOffer offer) {
        offerItems.stream()
                .filter(e -> e.getOffer().equals(offer))
                .findAny()
                .ifPresent(offerItems::remove);
    }

    public void setMarketPrice(double marketPrice) {
        this.marketPrice.set(marketPrice);
    }

    public void reset() {
        clearFilterPredicates();
        amountFilterModel.reset();
    }

    public void setSelectAskCurrency(String currency) {
        selectedAskCurrency.set(currency);
        Predicate<OfferListItem> predicate = item -> item.getOffer().getAskAsset().getCode().equals(currency);
        setCurrencyPredicate(predicate);
    }

    public void setSelectBidCurrency(String currency) {
        selectedBidCurrency.set(currency);
        Predicate<OfferListItem> predicate = item -> item.getOffer().getBidAsset().getCode().equals(currency);
        setCurrencyPredicate(predicate);
    }

    public OfferListItem toOfferListItem(SwapOffer offer) {
        return new OfferListItem(offer, marketPrice);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Package private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    void clearFilterPredicates() {
        listFilterPredicates.clear();

        amountFilterModel.clearFilterPredicates();
        applyListFilterPredicates();
    }

    void applyListFilterPredicates() {
        listFilterPredicates.stream().reduce(Predicate::and)
                .ifPresent(filteredItems::setPredicate);
    }

    void applyBaseCurrency() {
        filteredItems.stream().findAny().ifPresent(o -> baseCurrency = o.getOffer().getBaseCurrency());
    }

    private void setCurrencyPredicate(Predicate<OfferListItem> predicate) {
        clearFilterPredicates();
        listFilterPredicates.add(predicate);
        currencyPredicate = predicate;
        applyListFilterPredicates();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Static
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static Tuple2<String, Double> getPriceTuple(double fixPrice, Optional<Double> marketBasedPrice, double marketPrice) {
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
        return new Tuple2<>(displayString, price);
    }

    public static double getPrice(double fixPrice, Optional<Double> marketBasedPrice, Optional<Double> marketPrice) {
        return marketBasedPrice.map(percentage -> marketPrice
                .map(marketPrice1 -> marketPrice1 * (1 + percentage)).orElse(fixPrice))
                .orElse(fixPrice);
    }

    public static long getMinQuoteAmountValue(List<OfferListItem> offers, Double marketPrice) {
        return offers.stream()
                .mapToLong(o -> getMinQuoteAmount(o.getOffer().getBaseAsset().getAmount(),
                        o.getOffer().getMinAmountAsPercentage(),
                        o.getOffer().getMarketBasedPrice(),
                        marketPrice))
                .min()
                .orElse(0);
    }

    public static long getMaxQuoteAmountValue(List<OfferListItem> offers, Double marketPrice) {
        return offers.stream()
                .mapToLong(o -> getQuoteAmount(o.getOffer().getBaseAsset().getAmount(),
                        o.getOffer().getMarketBasedPrice(),
                        marketPrice))
                .max()
                .orElse(0);
    }

    public static double getLowestPrice(List<SwapOffer> offers, Optional<Double> marketPrice) {
        return offers.stream()
                .mapToDouble(offer -> getPrice(offer.getFixPrice(), offer.getMarketBasedPrice(), marketPrice))
                .min()
                .orElse(0);
    }

    public static double getHighestPrice(List<SwapOffer> offers, Optional<Double> marketPrice) {
        return offers.stream()
                .mapToDouble(offer -> getPrice(offer.getFixPrice(), offer.getMarketBasedPrice(), marketPrice))
                .max()
                .orElse(0);
    }

    public static long getMinQuoteAmount(long baseAmount, Optional<Double> minAmountAsPercentage, Optional<Double> marketBasedPrice, double marketPrice) {
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

    public static int comparePrice(Double selfPrice, Double otherPrice, String bidAssetCode, String quoteCurrencyCode) {
        if (bidAssetCode.equals(quoteCurrencyCode)) {
            return Double.compare(otherPrice, selfPrice);
        } else {
            return Double.compare(selfPrice, otherPrice);
        }
    }

    public static String getFormattedQuoteAmount(long baseAmount, Optional<Double> minAmountAsPercentage,
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

    public static long getQuoteAmount(long baseAmount, Optional<Double> marketBasedPrice, double marketPrice) {
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
}
