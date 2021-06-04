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

import io.reactivex.rxjava3.disposables.Disposable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import lombok.Getter;
import misq.finance.offer.OfferbookRepository;
import misq.jfx.common.Model;
import misq.marketprice.MarketPriceService;
import misq.presentation.offer.OfferbookEntity;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// Handled jfx only concerns, others which can be re-used by other frontends are in OfferbookEntity
public class OfferbookModel extends OfferbookEntity implements Model {

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
    private final StringProperty selectedAskCurrencyProperty = new SimpleStringProperty();
    @Getter
    private final StringProperty selectedBidCurrencyProperty = new SimpleStringProperty();
    @Getter
    private final DoubleProperty marketPriceProperty = new SimpleDoubleProperty();
    @Getter
    private final ObservableList<String> currenciesProperty = FXCollections.observableArrayList("BTC", "USD", "EUR", "XMR", "USDT");
    @Getter
    private final RangeFilterModel amountFilterModel;
    private Disposable offerEntityAddedDisposable, offerEntityRemovedDisposable, marketPriceDisposable;

    public OfferbookModel(OfferbookRepository offerbookRepository, MarketPriceService marketPriceService) {
        super(offerbookRepository, marketPriceService);

        amountFilterModel = new RangeFilterModel(this);
    }

    public void initialize() {
        super.initialize();
        selectedAskCurrencyProperty.set("BTC");
        selectedBidCurrencyProperty.set("USD");
        amountFilterModel.initialize();
    }

    public void activate() {
        super.activate();

        offerItems.clear();
        offerItems.addAll(offerEntities.stream()
                .map(OfferListItem -> new OfferListItem(OfferListItem.getOffer(), OfferListItem.getMarketPriceSubject()))
                .collect(Collectors.toList()));

        applyBaseCurrency();

        resetFilter();
        Predicate<OfferListItem> predicate = item -> item.getOffer().getAskAsset().getCode().equals(selectedAskCurrencyProperty.get());
        setCurrencyPredicate(predicate);
        amountFilterModel.activate();

        offerEntityAddedDisposable = offerEntityAddedSubject.subscribe(OfferListItem -> {
            offerItems.add(new OfferListItem(OfferListItem.getOffer(), OfferListItem.getMarketPriceSubject()));
        }, Throwable::printStackTrace);

        offerEntityRemovedDisposable = offerEntityRemovedSubject.subscribe(OfferListItem -> {
            offerItems.stream()
                    .filter(e -> e.getOffer().equals(OfferListItem.getOffer()))
                    .findAny()
                    .ifPresent(offerItems::remove);
        }, Throwable::printStackTrace);

        marketPriceDisposable = marketPriceSubject.subscribe(marketPriceProperty::set, Throwable::printStackTrace);
    }

    public void deactivate() {
        super.deactivate();
        amountFilterModel.deactivate();

        offerEntityAddedDisposable.dispose();
        offerEntityRemovedDisposable.dispose();
        marketPriceDisposable.dispose();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void resetFilter() {
        clearFilterPredicates();
        amountFilterModel.reset();
    }

    public void setSelectAskCurrency(String currency) {
        selectedAskCurrencyProperty.set(currency);
        Predicate<OfferListItem> predicate = item -> item.getOffer().getAskAsset().getCode().equals(currency);
        setCurrencyPredicate(predicate);
    }

    public void setSelectBidCurrency(String currency) {
        selectedBidCurrencyProperty.set(currency);
        Predicate<OfferListItem> predicate = item -> item.getOffer().getBidAsset().getCode().equals(currency);
        setCurrencyPredicate(predicate);
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

 /*   private OfferListItem toOfferListItem(SwapOffer offer) {
        return new OfferListItem(offer, marketPrice);
    }*/


}
