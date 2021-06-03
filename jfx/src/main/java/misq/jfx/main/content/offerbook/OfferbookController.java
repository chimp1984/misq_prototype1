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

import javafx.geometry.Bounds;
import lombok.Getter;
import misq.api.Api;
import misq.finance.offer.Offer;
import misq.finance.offer.OfferbookRepository;
import misq.finance.swap.offer.SwapOffer;
import misq.jfx.common.Controller;
import misq.jfx.main.content.ContentViewController;
import misq.jfx.main.content.createoffer.CreateOfferController;
import misq.jfx.main.content.offerbook.details.OfferDetailsController;
import misq.jfx.overlay.OverlayController;
import misq.marketprice.MarketPriceService;
import misq.presentation.offer.OfferListItem;

// As all controllers are created we do not do anything in the constructors beside assigning fields.
// initialize starts  MVC group up. onViewAdded is called when the view got added to the stage.
// onViewRemoved when the view got removed from the stage.
public class OfferbookController implements MarketPriceService.Listener, OfferbookRepository.Listener, Controller {
    private OfferbookModel model;
    @Getter
    private OfferbookView view;
    @Getter
    private final Api api;
    private final ContentViewController contentViewController;
    private final OverlayController overlayController;

    public OfferbookController(Api api, ContentViewController contentViewController, OverlayController overlayController) {
        this.api = api;
        this.contentViewController = contentViewController;
        this.overlayController = overlayController;
    }

    @Override
    public void initialize() {
        this.model = new OfferbookModel(api);
        model.initialize();
        this.view = new OfferbookView(model, this);
    }

    @Override
    public void onViewAdded() {
        model.activate();
        api.getMarketPriceService().addListener(this);
        api.getOfferbookRepository().addListener(this);
    }

    @Override
    public void onViewRemoved() {
        api.getMarketPriceService().removeListener(this);
        api.getOfferbookRepository().removeListener(this);
        model.deactivate();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Domain events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onOfferAdded(Offer offer) {
        if (offer instanceof SwapOffer) {
            model.addOffer((SwapOffer) offer);
        }
    }

    @Override
    public void onOfferRemoved(Offer offer) {
        if (offer instanceof SwapOffer) {
            model.removeOffer((SwapOffer) offer);
        }
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
        overlayController.show(new CreateOfferController());
    }

    public void onTakeOffer(OfferListItem item) {
    }

    public void onShowMakerDetails(OfferListItem item, Bounds boundsInParent) {
        overlayController.show(new OfferDetailsController(item, boundsInParent));
    }
}
