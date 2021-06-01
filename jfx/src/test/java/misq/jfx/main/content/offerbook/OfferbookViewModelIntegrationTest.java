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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import misq.presentation.offer.OfferbookModel;

public class OfferbookViewModelIntegrationTest {
    OfferbookModel model;
    private DoubleProperty marketPrice = new SimpleDoubleProperty();

   // @Test
   /* public void testOffers() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        TestApplicationLauncher.launch().whenComplete((application, throwable) -> {
            Pane root = application.root;
            OfferbookView view = new OfferbookView();
            model = view.getController();
            root.getChildren().add(view.getRoot());

            latch.countDown();
        });
        latch.await(500, TimeUnit.SECONDS);
        Thread.sleep(100);
        List<OfferItem> offerItems = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            OfferItem offerItem = getRandomOffer(i);
            offerItems.add(offerItem);
        }

        updateMarketPrice();
        UserThread.runPeriodically(this::updateMarketPrice, 1000);
        model.setOfferItems(offerItems);
        Thread.sleep(100000);
    }

    private void updateMarketPrice() {
        double rand = new Random().nextInt(10000) / 10000d;
        marketPrice.set(50000 + 1000 * rand);
    }

    private String getFormattedMarketBasedPrice(double premium, double marketPrice) {
        double percentagePrice = marketPrice * (1 + premium);
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(percentagePrice);
    }

    private OfferItem getRandomOffer(int i) {
        double rand = new Random().nextInt(10000) / 10000d;
        double premium = 0.02 * rand;
        StringProperty price = new SimpleStringProperty(getFormattedMarketBasedPrice(premium, marketPrice.get()));
      *//*  OfferItem offerItem = new OfferItem(UUID.randomUUID().toString(),
                premium,
                "3213.22",
                price,
                "maker " + i,
                "Zelle/Multisig",
                marketPrice,
                this::getFormattedMarketBasedPrice);*//*
        return null;
    }*/
}
