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
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;
import misq.TestApplicationLauncher;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

@Slf4j
public class MarketPriceIntegrationTest {

    private MockMarketPriceConfig config;
    private MarketPriceViewModel model;

    @Test
    public void testMarketPrice() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        TestApplicationLauncher.launch().whenComplete((application, throwable) -> {
            Pane root = application.root;
            MarketPriceView view = new MarketPriceView();
            model = view.getModel();
            config = new MockMarketPriceConfig(model);
            config.presentation.period = 100;
            root.getChildren().add(view);

            Platform.runLater(() -> assertFalse(model.markets.isEmpty()));
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);

        CountDownLatch latch2 = new CountDownLatch(1);
        while (latch2.getCount() > 0) {
            if (!model.marketPrice.get().isEmpty()) {

                latch2.countDown();
            }
            Thread.sleep(config.presentation.period);
        }
        latch2.await(2, TimeUnit.SECONDS);

        assertFalse(model.marketPrice.get().isEmpty());

        String pre = model.marketPrice.get();
        model.onRefreshMarketPrice();
        Platform.runLater(() -> assertNotEquals(pre, model.marketPrice.get()));
    }
}
