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

package misq.jfx.main.content.offerbook;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.common.AViewModel;
import misq.jfx.common.ViewModel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class OfferbookViewModel extends AViewModel {
    final ObservableList<OfferItem> offerItems = FXCollections.observableArrayList();
    @Getter
    final FilteredList<OfferItem> filteredItems = new FilteredList<>(offerItems);
    final SortedList<OfferItem> sortedItems = new SortedList<>(filteredItems);
    final ObservableList<String> currencies = FXCollections.observableArrayList("BTC", "USD", "EUR", "XMR", "USDT");

    private String selectedAskCurrency;
    private String selectedBidCurrency;
    final DoubleProperty marketPrice = new SimpleDoubleProperty(0);
    @Setter
    AmountFormatter amountFormatter;
    final ObjectProperty<Number> highBaseAmount = new SimpleObjectProperty<>(Long.MAX_VALUE);
    final ObjectProperty<Number> lowBaseAmount = new SimpleObjectProperty<>(0);
    final ObjectProperty<Number> highQuoteAmount = new SimpleObjectProperty<>(Long.MAX_VALUE);
    final ObjectProperty<Number> lowQuoteAmount = new SimpleObjectProperty<>(0);
    final ObjectProperty<Number> highPrice = new SimpleObjectProperty<>(Long.MAX_VALUE);
    final ObjectProperty<Number> lowPrice = new SimpleObjectProperty<>(0);
    final StringProperty baseCurrencyCode = new SimpleStringProperty("");
    final StringProperty quoteCurrencyCode = new SimpleStringProperty("");
    @Setter
    Function<Set<String>, Long> smallestBaseAmountSupplier;
    @Setter
    Function<Set<String>, Long> largestBaseAmountSupplier;
    @Setter
    Function<Set<String>, Long> smallestQuoteAmountSupplier;
    @Setter
    Function<Set<String>, Long> largestQuoteAmountSupplier;
    @Setter
    Function<Set<String>, Double> lowestPriceSupplier;
    @Setter
    Function<Set<String>, Double> highestPriceSupplier;

    public OfferbookViewModel() {
        super();

        highBaseAmount.addListener(observable -> applyFilter());
        lowBaseAmount.addListener(observable -> applyFilter());
        highQuoteAmount.addListener(observable -> applyFilter());
        lowQuoteAmount.addListener(observable -> applyFilter());
        highPrice.addListener(observable -> applyFilter());
        lowPrice.addListener(observable -> applyFilter());
    }

    public void setOfferItems(List<OfferItem> list) {
        this.offerItems.clear();
        this.offerItems.addAll(list);
        offerItems.sort(OfferItem::compareTo);
    }

    public void onOfferItemAdded(OfferItem item) {
        Platform.runLater(() -> {
            this.offerItems.add(item);
            offerItems.sort(OfferItem::compareTo);
        });
    }

    public void onOfferListItemRemoved(String offerId) {
        Platform.runLater(() -> offerItems.stream()
                .filter(e -> e.getId().equals(offerId))
                .findAny()
                .ifPresent(offerItems::remove));
    }

    public void onMarketPriceUpdate(double price) {
        Platform.runLater(() -> marketPrice.set(price));
    }

    @Override
    public void onConstructView(ViewModel viewModel) {
        super.onConstructView(viewModel);
    }

    @Override
    public void onViewAdded() {
        super.onViewAdded();
    }

    @Override
    public void onViewRemoved() {
        super.onViewRemoved();
    }


    void onAskCurrencySelected(String selectedAskCurrency) {
        this.selectedAskCurrency = selectedAskCurrency;
        applyFilter();
    }

    void onBidCurrencySelected(String selectedBidCurrency) {
        this.selectedBidCurrency = selectedBidCurrency;
        applyFilter();
    }


    private void applyFilter() {
        Predicate<OfferItem> predicate = item -> item.getBidAssetCode().equals(selectedAskCurrency) &&
                item.getAskAssetCode().equals(selectedBidCurrency) &&
                item.getBaseAmountAsLong() <= highBaseAmount.get().longValue() &&
                item.getMinBaseAmountAsLong() >= lowBaseAmount.get().longValue() &&
                item.getQuoteAmountAsLong() <= highQuoteAmount.get().longValue() &&
                item.getMinQuoteAmountAsLong() >= lowQuoteAmount.get().longValue() &&
                item.getPriceAsDouble().get() <= highPrice.get().doubleValue() &&
                item.getPriceAsDouble().get() >= lowPrice.get().doubleValue();
        filteredItems.setPredicate(predicate);
    }


    public Number getSmallestBaseAmount() {
        return smallestBaseAmountSupplier.apply(getOfferIdSet(filteredItems));
    }

    public Number getLargestBaseAmount() {
        return largestBaseAmountSupplier.apply(getOfferIdSet(filteredItems));
    }

    public Number getSmallestQuoteAmount() {
        return smallestQuoteAmountSupplier.apply(getOfferIdSet(filteredItems));
    }

    public Number getLargestQuoteAmount() {
        return largestQuoteAmountSupplier.apply(getOfferIdSet(filteredItems));
    }

    public Number getLowestPrice() {
        return lowestPriceSupplier.apply(getOfferIdSet(filteredItems));
    }

    public Number getHighestPrice() {
        return highestPriceSupplier.apply(getOfferIdSet(filteredItems));
    }

    private Optional<String> getOptionalBaseCurrencyCode() {
        if (!filteredItems.isEmpty()) {
            baseCurrencyCode.set(filteredItems.get(0).getBaseCurrencyCode());
        }
        return baseCurrencyCode.get().isEmpty() ? Optional.empty() : Optional.of(baseCurrencyCode.get());
    }

    private Optional<String> getOptionalQuoteCurrencyCode() {
        if (!filteredItems.isEmpty()) {
            quoteCurrencyCode.set(filteredItems.get(0).getQuoteCurrencyCode());
        }
        return quoteCurrencyCode.get().isEmpty() ? Optional.empty() : Optional.of(quoteCurrencyCode.get());
    }

    public String getFormattedBaseAmount(Number amount) {
        return getOptionalBaseCurrencyCode().map(baseCurrencyCode -> amountFormatter.get(amount.longValue(), baseCurrencyCode))
                .orElse("");
    }

    public String getFormattedQuoteAmount(Number amount) {
        return getOptionalQuoteCurrencyCode().map(quoteCurrencyCode -> amountFormatter.get(amount.longValue(), quoteCurrencyCode))
                .orElse("");
    }

    public String getFormattedPrice(Number price) {
        return getOptionalBaseCurrencyCode().map(baseCurrencyCode -> amountFormatter.get(price.longValue(), baseCurrencyCode))
                .orElse("");
    }

    private Set<String> getOfferIdSet(Collection<OfferItem> offerItems) {
        return offerItems.stream().map(OfferItem::getId).collect(Collectors.toSet());
    }

}
