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

package misq.jfx.main;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.subjects.PublishSubject;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class MarketPriceViewModel {
    @Setter
    public Runnable refreshMarketPriceHandler;

    public ObservableList<String> markets = FXCollections.observableArrayList();
    public StringProperty marketPrice = new SimpleStringProperty("");
    public StringProperty selectedMarket = new SimpleStringProperty();

    public DisposableObserver<Map<String, Integer>> marketPriceObserver;
    public PublishSubject<Boolean> refreshMarketPriceEvent = PublishSubject.create();


    public MarketPriceViewModel() {
        marketPriceObserver = new DisposableObserver<>() {
            @Override
            public void onNext(@NonNull Map<String, Integer> map) {
                Platform.runLater(() -> {
                    MarketPriceViewModel.this.marketPrice.set(format(map.get(selectedMarket.get())));
                });
            }

            @Override
            public void onError(@NonNull Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        };
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

    public void onRefreshMarketPrice() {
        refreshMarketPriceHandler.run();
        refreshMarketPriceEvent.onNext(true);
    }

    private String format(long marketPrice) {
        return String.format("%f", marketPrice / 1000d);
    }

    public void onChangeSubscribedMarket(String newValue) {
        selectedMarket.set(newValue);
    }
}
