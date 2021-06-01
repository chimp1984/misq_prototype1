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

package misq.presentation.marketprice;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class MockMarketPriceService implements MarketPriceService {
    public interface Listener {
        void onMarketPriceChanged(double marketPrice);
    }

    @Getter
    private double marketPrice;

    private final Set<MockMarketPriceService.Listener> listeners = new CopyOnWriteArraySet<>();

    public MockMarketPriceService() {
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

    @Override
    public void addListener(MockMarketPriceService.Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(MockMarketPriceService.Listener listener) {
        listeners.remove(listener);
    }
}
