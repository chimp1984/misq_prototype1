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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.presentation.Controller;

@Slf4j
public class OfferbookController implements Controller {
    @Getter
    private final OfferbookModel model;

    public OfferbookController(OfferbookModel model) {
        this.model = model;
    }

    public void onCreateView() {
        model.initialize();
    }

    @Override
    public void onViewAdded() {
        model.activate();
    }

    @Override
    public void onViewRemoved() {
        model.deactivate();
    }

    public void onSelectAskCurrency(String currency) {
        model.setSelectAskCurrency(currency);
    }

    public void onSelectBidCurrency(String currency) {
        model.setSelectBidCurrency(currency);
    }

    public void onFlipCurrencies() {
        model.reset();
    }

    public void onCreateOffer() {
    }

    public void onTakeOffer(OfferListItem item) {
    }

    public void onShowMakerDetails(OfferListItem item) {
    }
}
