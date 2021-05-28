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

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class MockMarketPricePresentation {
    public Map<String, Integer> map = new HashMap<>();

    @Setter
    public Consumer<Map<String, Integer>> marketPriceConsumer;
    int period = 10;

    public MockMarketPricePresentation() {
        init();
    }

    public void onRefreshMarketPrice() {
        updateMarketPrice();
    }

    void init() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                updateMarketPrice();
            }
        };
        new Timer().scheduleAtFixedRate(timerTask, 0, period);
        updateMarketPrice();
    }

    void updateMarketPrice() {
        map.put("BTC/USD", new Random().nextInt(100000000));
        map.put("BTC/EUR", new Random().nextInt(1000000));
        map.put("BTC/CHF", new Random().nextInt(10000));

        if (marketPriceConsumer != null)
            marketPriceConsumer.accept(map);
    }

    public Set<String> getMarkets() {
        return map.keySet();
    }
}
