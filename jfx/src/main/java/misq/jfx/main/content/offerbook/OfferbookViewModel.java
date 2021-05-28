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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.common.AViewModel;
import misq.jfx.common.ViewModel;

import java.util.List;
import java.util.Optional;

@Slf4j
public class OfferbookViewModel extends AViewModel {
    public static void setListener(Listener listener) {
        OfferbookViewModel.listener.of(listener);
    }

    private static Optional<Listener> listener = Optional.empty();

    @Override
    public Optional<Listener> getListener() {
        return listener;
    }

    ObservableList<OfferListItem> offerListItems = FXCollections.observableArrayList();

    public OfferbookViewModel() {
        super();
    }

    public void onOfferListItemsChange(List<OfferListItem> list) {
        this.offerListItems.clear();
        this.offerListItems.addAll(list);
    }

    @Override
    public void onConstructed(ViewModel viewModel) {
        super.onConstructed(viewModel);
    }

    @Override
    public void onInitialized() {
        super.onInitialized();
    }

    @Override
    public void onActivated() {
    }

    @Override
    public void onDeactivated() {
    }

    @Override
    public void onDestructed() {
    }


}
