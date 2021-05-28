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

package misq.jfx.main.content.offerbook;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.Pane;
import misq.TestApplicationLauncher;
import misq.jfx.utils.UserThread;
import org.junit.Test;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class OfferbookViewModelTest {
    OfferbookViewModel model;

    @Test
    public void testOffers() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        TestApplicationLauncher.launch().whenComplete((application, throwable) -> {
            Pane root = application.root;
            OfferbookView view = new OfferbookView();

            model = view.getModel();
            root.getChildren().add(view.getRoot());

         /*   Platform.runLater(() -> {
                ObservableList<OfferListItem> offerListItems = model.offerListItems;
                assertTrue(offerListItems.isEmpty());
            });*/
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);
        Thread.sleep(100);

        List<OfferListItem> offerListItems = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            offerListItems.add(getRandomOffer());
        }
        model.onOfferListItemsChange(offerListItems);
        Thread.sleep(100000);
    }

    private OfferListItem getRandomOffer() {
        String makerAddress = "maker " + new Random().nextInt(1000);
        AssetListItem askAsset = new AssetListItem("USD", true, new Random().nextInt(50000000) + 100000, List.of("Zelle"));
        AssetListItem bidAsset = new AssetListItem("BTC", false, new Random().nextInt(100000000) + 1000000, List.of());
        String amount = String.valueOf(askAsset.amount / 1000000d).substring(0, 3);
        String maker = makerAddress;
        String details = "Zelle/Multisig";

        StringProperty price = new SimpleStringProperty("");
        //String.valueOf(((double) askAsset.amount) / ((double) bidAsset.amount)).substring(0, 4);

        UserThread.runPeriodically(() -> {
            double rand = new Random().nextInt(10000) / 10000d;
            double marketPrice = 50000 + 1000 * rand;
            double percentageBasedPrice = 0.02;
            double percentagePrice = marketPrice * (1 + percentageBasedPrice);
            DecimalFormat df = new DecimalFormat("#.##");
            String formatted = df.format(percentagePrice);
            price.set(formatted);
        }, 1000);

        return new OfferListItem(amount, price, maker, details);
    }
}
