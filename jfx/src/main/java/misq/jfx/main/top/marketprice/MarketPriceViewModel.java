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

package misq.jfx.main.top.marketprice;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.common.AViewModel;
import misq.jfx.common.ViewModel;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

@Slf4j
public class MarketPriceViewModel extends AViewModel {

    @Setter
    public Runnable refreshMarketPriceHandler;

    public ObservableList<String> markets = FXCollections.observableArrayList();
    public StringProperty marketPrice = new SimpleStringProperty("");
    public StringProperty selectedMarket = new SimpleStringProperty("BTC/USD");


    public MarketPriceViewModel() {
        super();
    }

    @Override
    public void onConstructed(ViewModel viewModel) {
        super.onConstructed(viewModel);
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

    public void onMarketPriceChange(Map<String, Integer> map) {
        Platform.runLater(() -> {
            this.marketPrice.set(format(map.get(selectedMarket.get())));
        });
    }

    public void setMarkets(Set<String> markets) {
        Platform.runLater(() -> {
            this.markets.addAll(markets);
            selectedMarket.set(new ArrayList<>(markets).get(0));
        });
    }

    void onRefreshMarketPrice() {
        if (refreshMarketPriceHandler != null)
            refreshMarketPriceHandler.run();
    }

    void onChangeSubscribedMarket(String newValue) {
        selectedMarket.set(newValue);
    }


    private String format(long marketPrice) {
        return String.format("%f", marketPrice / 1000d);
    }

}
