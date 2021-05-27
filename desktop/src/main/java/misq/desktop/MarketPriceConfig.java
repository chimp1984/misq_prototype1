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
import misq.jfx.main.MarketPriceViewModel;
import misq.presentation.MarketsPresentation;

@Slf4j
public class MarketPriceConfig {

    public MarketPriceConfig(MarketPriceViewModel viewModel) {
        MarketsPresentation presentation = new MarketsPresentation();

        // rx version
        presentation.marketPricePublisher.subscribe(viewModel.marketPriceObserver);
        viewModel.refreshMarketPriceEvent.subscribe(presentation.refreshMarketPriceHandler);

        // plain version with consumer/runnable
        viewModel.setRefreshMarketPriceHandler(presentation::onRefreshMarketPrice);
        presentation.setMarketPriceConsumer(viewModel::onMarketPriceChange);

        viewModel.setMarkets(presentation.getMarkets());
    }
}
