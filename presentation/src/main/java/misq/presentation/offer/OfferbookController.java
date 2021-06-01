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
import misq.finance.offer.Offer;
import misq.finance.offer.Offerbook;
import misq.finance.swap.offer.SwapOffer;
import misq.presentation.Controller;
import misq.presentation.marketprice.MarketPriceService;
import misq.presentation.marketprice.MockMarketPriceService;

@Slf4j
public class OfferbookController implements Controller, Offerbook.Listener, MockMarketPriceService.Listener {
    @Getter
    private final OfferbookModel model;
    private final Offerbook offerbook;
    private final MarketPriceService marketPriceService;

    public OfferbookController(OfferbookModel model, Offerbook offerbook, MarketPriceService marketPriceService) {
        this.model = model;
        this.offerbook = offerbook;
        this.marketPriceService = marketPriceService;
    }

    @Override
    public void onCreateView() {
        model.initialize();
    }

    @Override
    public void onViewAdded() {
        model.activate();
        marketPriceService.addListener(this);
        offerbook.addListener(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Domain events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onViewRemoved() {
        marketPriceService.removeListener(this);
        offerbook.removeListener(this);
        model.deactivate();
    }

    @Override
    public void onOfferAdded(Offer offer) {
        if (offer instanceof SwapOffer) {
            model.addOffer((SwapOffer) offer);
        }
    }

    @Override
    public void onOfferRemoved(Offer offer) {
        model.removeOffer((SwapOffer) offer);
    }

    @Override
    public void onMarketPriceChanged(double marketPrice) {
        model.setMarketPrice(marketPrice);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // View events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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
