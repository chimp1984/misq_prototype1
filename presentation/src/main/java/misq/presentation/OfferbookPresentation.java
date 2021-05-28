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

import lombok.extern.slf4j.Slf4j;
import misq.finance.Asset;
import misq.finance.TransferType;
import misq.finance.offer.Offer;
import misq.finance.swap.SwapProtocolType;
import misq.finance.swap.offer.SwapOffer;
import misq.p2p.Address;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class OfferbookPresentation {
    public List<Offer> offers = new ArrayList<>();
    private final TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            log.error("timerTask run");
            // update();
        }
    };
    private final Timer timer = new Timer();
    ;

    public void setOffersConsumer(Consumer<List<Offer>> offersConsumer) {
        this.offersConsumer = offersConsumer;
    }

    public Consumer<List<Offer>> offersConsumer;

    public OfferbookPresentation() {
    }

    public void onViewAdded() {
        for (int i = 0; i < 5; i++) {
            offers.add(getRandomOffer());
        }

        timer.scheduleAtFixedRate(timerTask, 0, 2000);
        update();
    }

    private void update() {
        for (int i = 0; i < 10; i++) {
            offers.add(getRandomOffer());
        }
        if (offersConsumer != null)
            offersConsumer.accept(offers);
    }

    private Offer getRandomOffer() {
        Address makerAddress = new Address("makerAddress" + new Random().nextInt(1000));
        Asset askAsset = new Asset("USD", true, new Random().nextInt(50000000) + 100000, List.of(TransferType.ZELLE));
        Asset bidAsset = new Asset("BTC", false, new Random().nextInt(100000000) + 1000000, List.of());
        return new SwapOffer(List.of(SwapProtocolType.MULTISIG),
                makerAddress, bidAsset, askAsset, Optional.empty());
    }

    public void stop() {
        log.error("onDestructed");
        timer.cancel();
        // timerTask.cancel();
    }
}
