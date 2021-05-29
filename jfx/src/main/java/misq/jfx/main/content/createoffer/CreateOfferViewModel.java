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

package misq.jfx.main.content.createoffer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.common.AViewModel;
import misq.jfx.common.ViewModel;
import misq.jfx.main.content.offerbook.OfferListItem;

import java.util.List;

@Slf4j
public class CreateOfferViewModel extends AViewModel {
    @Getter
    ObservableList<OfferListItem> offerListItems = FXCollections.observableArrayList();

    public CreateOfferViewModel() {
        super();
    }

    public void setOfferListItems(List<OfferListItem> list) {
        this.offerListItems.clear();
        this.offerListItems.addAll(list);
    }

    public void onOfferListItemAdded(OfferListItem item) {
        this.offerListItems.add(item);
    }

    public void onOfferListItemRemoved(String offerId) {
        offerListItems.stream()
                .filter(e -> e.getId().equals(offerId))
                .findAny()
                .ifPresent(o -> offerListItems.remove(o));
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

    public void onMarketPriceChange() {

    }
}
