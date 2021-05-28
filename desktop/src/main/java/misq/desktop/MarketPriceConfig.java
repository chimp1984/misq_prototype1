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
import misq.jfx.common.AViewModel;
import misq.jfx.common.LifeCycle;
import misq.jfx.common.ViewModel;
import misq.jfx.main.top.marketprice.MarketPriceViewModel;
import misq.presentation.MarketsPresentation;

@Slf4j
public class MarketPriceConfig implements LifeCycle {
    private static MarketPriceConfig marketPriceConfig;

    static void setup() {
        MarketPriceViewModel.setListener(new AViewModel.Listener() {
            @Override
            public void onConstructed(ViewModel viewModel) {
                marketPriceConfig = new MarketPriceConfig((MarketPriceViewModel) viewModel);
            }

            @Override
            public void onInitialized() {
                marketPriceConfig.onInitialized();
            }

            @Override
            public void onActivated() {
                marketPriceConfig.onActivated();
            }

            @Override
            public void onDeactivated() {
                marketPriceConfig.onDeactivated();
            }

            @Override
            public void onDestructed() {
                marketPriceConfig.onDestructed();
                marketPriceConfig = null;
            }
        });
    }

    private final MarketsPresentation presentation;
    private final MarketPriceViewModel viewModel;

    public MarketPriceConfig(MarketPriceViewModel viewModel) {
        this.viewModel = viewModel;
        presentation = new MarketsPresentation();

        viewModel.setRefreshMarketPriceHandler(presentation::onRefreshMarketPrice);
        presentation.setMarketPriceConsumer(viewModel::onMarketPriceChange);

        viewModel.setMarkets(presentation.getMarkets());
    }

    @Override
    public void onConstructed(ViewModel viewModel) {
    }

    @Override
    public void onInitialized() {

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
