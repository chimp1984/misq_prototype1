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

package misq.jfx.main.content.markets;

import javafx.application.Platform;
import lombok.Getter;
import misq.api.Api;
import misq.jfx.common.Controller;
import misq.marketprice.MarketPriceService;

public class MarketsController implements Controller {
    private final Api api;
    private MarketsModel model;
    private MarketPriceService marketPriceService;
    @Getter
    private MarketsView view;

    public MarketsController(Api api) {
        this.api = api;
    }

    @Override
    public void initialize() {
        this.model = new MarketsModel();
        this.view = new MarketsView(model, this);
        this.marketPriceService = api.getMarketPriceService();
    }

    @Override
    public void onViewAdded() {

    }

    @Override
    public void onViewRemoved() {

    }

    void onRefresh() {
        marketPriceService.requestPriceUpdate()
                .whenComplete((marketPrice, t) -> Platform.runLater(() -> model.setMarketPrice(marketPrice)));
    }
}
