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
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.common.AViewModel;
import misq.jfx.common.ViewModel;

import java.util.List;
import java.util.function.Predicate;

@Slf4j
public class OfferbookViewModel extends AViewModel {
    final ObservableList<OfferItem> offerItems = FXCollections.observableArrayList();
    final FilteredList<OfferItem> filteredItems = new FilteredList<>(offerItems);
    final SortedList<OfferItem> sortedItems = new SortedList<>(filteredItems);
    final ObservableList<String> currencies = FXCollections.observableArrayList("BTC", "USD", "EUR", "XMR", "USDT");
    private String selectedAskCurrency;
    private String selectedBidCurrency;
    final DoubleProperty marketPrice = new SimpleDoubleProperty(0);

    public OfferbookViewModel() {
        super();
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
                item.getAskAssetCode().equals(selectedBidCurrency);
        filteredItems.setPredicate(predicate);
    }
}
