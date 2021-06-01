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

package misq.desktop;

import lombok.extern.slf4j.Slf4j;
import misq.finance.offer.Offerbook;
import misq.jfx.JfxLauncher;
import misq.jfx.MvcInjector;
import misq.jfx.main.content.offerbook.OfferbookView;
import misq.presentation.marketprice.MarketPriceService;
import misq.presentation.marketprice.MockMarketPriceService;
import misq.presentation.offer.OfferbookController;
import misq.presentation.offer.OfferbookModel;

@Slf4j
public class Desktop {

    public Desktop() {
        launchApplication();
    }

    private void launchApplication() {
        JfxLauncher.launch()
                .whenComplete((success, throwable) -> {
                    initialize();
                });
    }

    private void initialize() {
        Offerbook.NetworkService networkService = new Offerbook.MockNetworkService();
        Offerbook offerbook = new Offerbook(networkService);
        MarketPriceService marketPriceService = new MockMarketPriceService();

        // Probably we will need some DI framework for wiring up the view with its controller and model
        OfferbookModel offerbookModel = new OfferbookModel(offerbook, marketPriceService);
        OfferbookController offerbookController = new OfferbookController(offerbookModel, offerbook, marketPriceService);
        MvcInjector.glue(OfferbookView.class, offerbookController);
    }
}
