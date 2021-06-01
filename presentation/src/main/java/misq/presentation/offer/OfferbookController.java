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

import lombok.extern.slf4j.Slf4j;
import misq.presentation.Controller;

import java.util.function.Predicate;

@Slf4j
public class OfferbookController implements Controller {
    private final OfferbookModel model;

    public OfferbookController(OfferbookModel model) {
        this.model = model;
    }

    public void onSelectAskCurrency(String currency) {
        model.getSelectedAskCurrency().set(currency);
        Predicate<OfferListItem> predicate = item -> item.getOffer().getAskAsset().getCode().equals(currency);
        model.setCurrencyPredicate(predicate);
        model.applyListFilterPredicates();
        applyBaseCurrency();
    }

    public void onSelectBidCurrency(String currency) {
        model.getSelectedBidCurrency().set(currency);
        Predicate<OfferListItem> predicate = item -> item.getOffer().getBidAsset().getCode().equals(currency);
        model.setCurrencyPredicate(predicate);
        model.applyListFilterPredicates();
        applyBaseCurrency();
    }

    public void onCreateOffer() {
    }

    public void onLowBaseAmountFilterChange(double percentage) {
        long value = model.lowBaseAmountPercentToValue(percentage);
        Predicate<OfferListItem> predicate = item -> item.getOffer().getBaseAsset().getAmount() >= value;
        model.setLowBaseAmountPredicate(predicate);
        model.applyListFilterPredicates();
        applyBaseCurrency();
    }

    public void onHighBaseAmountFilterChange(double percentage) {
        long value = model.highBaseAmountPercentToValue(percentage);
        Predicate<OfferListItem> predicate = item -> item.getOffer().getMinBaseAmount() <= value;
        model.setHighBaseAmountPredicate(predicate);
        model.applyListFilterPredicates();
        applyBaseCurrency();
    }


    private void applyBaseCurrency() {
        model.getFilteredItems().stream().findAny().ifPresent(o -> model.setBaseCurrency(o.getOffer().getBaseCurrency()));
    }
}
