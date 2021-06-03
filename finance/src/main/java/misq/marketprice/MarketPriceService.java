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

package misq.marketprice;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MarketPriceService {
    public interface Listener {
        void onMarketPriceChanged(double marketPrice);
    }

    @Getter
    private double marketPrice;

    private final Set<MarketPriceService.Listener> listeners = new CopyOnWriteArraySet<>();

    public CompletableFuture<Integer> requestPriceUpdate() {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS)
                .execute(() -> future.complete(new Random().nextInt(5000000)));
        return future;
    }

    public MarketPriceService() {
        marketPrice = 50000 + new Random().nextInt(10000) / 10000d;
     /*   new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                marketPrice = 50000 + new Random().nextInt(1000000) / 1000d;
                // log.error("price update {}", marketPrice);
                listeners.forEach(e -> e.onPriceUpdate(marketPrice));
            }
        }, 0, 1000);*/
    }

    public void addListener(MarketPriceService.Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(MarketPriceService.Listener listener) {
        listeners.remove(listener);
    }
}
