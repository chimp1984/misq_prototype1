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

package misq.presentation;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.subjects.PublishSubject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class MarketsPresentation {
    public Map<String, Integer> map = new HashMap<>();
    public PublishSubject<Map<String, Integer>> marketPricePublisher = PublishSubject.create();
    public DisposableObserver<Boolean> refreshMarketPriceHandler;

    @Setter
    public Consumer<Map<String, Integer>> marketPriceConsumer;

    public MarketsPresentation() {
        refreshMarketPriceHandler = new DisposableObserver<>() {
            @Override
            public void onNext(@NonNull Boolean b) {
                updateMarketPrice();
            }

            @Override
            public void onError(@NonNull Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        };

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                updateMarketPrice();
            }
        };
        new Timer().scheduleAtFixedRate(timerTask, 0, 2000);
        updateMarketPrice();
    }

    private void updateMarketPrice() {
        map.put("BTC/USD", new Random().nextInt(100000000));
        map.put("BTC/EUR", new Random().nextInt(1000000));
        map.put("BTC/CHF", new Random().nextInt(10000));

        marketPricePublisher.onNext(map);
        if (marketPriceConsumer != null)
            marketPriceConsumer.accept(map);
    }

    public Set<String> getMarkets() {
        return map.keySet();
    }

    public void onRefreshMarketPrice() {
        updateMarketPrice();
    }
}
