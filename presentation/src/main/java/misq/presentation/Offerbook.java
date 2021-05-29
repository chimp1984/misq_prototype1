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

import io.reactivex.rxjava3.subjects.PublishSubject;
import lombok.extern.slf4j.Slf4j;
import misq.finance.Asset;
import misq.finance.TransferType;
import misq.finance.offer.Offer;
import misq.finance.swap.SwapProtocolType;
import misq.finance.swap.offer.SwapOffer;
import misq.p2p.Address;
import misq.p2p.NetworkId;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

@Slf4j
public class Offerbook {
    public List<Offer> offers = new ArrayList<>();
    public Set<Consumer<Offer>> addedOfferConsumers = new CopyOnWriteArraySet<>();
    public Set<Consumer<Offer>> removedOfferConsumers = new CopyOnWriteArraySet<>();

    public Offerbook() {
        marketPriceService = new MockMarketPriceService();
        networkService = new MockNetworkService();
        for (int i = 0; i < 20; i++) {
            addOffer();
        }
    }

    public void addAddedOffersConsumer(Consumer<Offer> consumer) {
        addedOfferConsumers.add(consumer);
    }

    public void removeAddedOffersConsumer(Consumer<Offer> consumer) {
        addedOfferConsumers.remove(consumer);
    }

    public void addRemovedOffersConsumer(Consumer<Offer> consumer) {
        removedOfferConsumers.add(consumer);
    }

    public void removeRemovedOffersConsumer(Consumer<Offer> consumer) {
        removedOfferConsumers.remove(consumer);
    }

    public String getFormattedMarketBasedPrice(double premium, double marketPrice) {
        double percentagePrice = marketPrice * (1 + premium);
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(percentagePrice);
    }


    private void addOffer() {
        Offer offer = getRandomOffer();
        offers.add(offer);
        addedOfferConsumers.forEach(consumer -> consumer.accept(offer));
    }

    private void removeOffer() {
        if (offers.isEmpty()) {
            return;
        }
        int index = new Random().nextInt(offers.size());
        Offer removed = offers.remove(index);
        removedOfferConsumers.forEach(consumer -> consumer.accept(removed));
    }

    private Offer getRandomOffer() {
        NetworkId makerNetworkId = new NetworkId(Address.localHost(1000 + new Random().nextInt(1000)), null, "default");
        Asset askAsset = new Asset("USD", true, new Random().nextInt(50000000) + 100000, List.of(TransferType.ZELLE));
        Asset bidAsset = new Asset("BTC", false, new Random().nextInt(100000000) + 1000000, List.of());
        return new SwapOffer(List.of(SwapProtocolType.MULTISIG),
                makerNetworkId, bidAsset, askAsset, Optional.empty());
    }


    public final MockMarketPriceService marketPriceService;
    private final MockNetworkService networkService;

    public class MockMarketPriceService {
        public final PublishSubject<Double> publishSubject;

        public MockMarketPriceService() {
            publishSubject = PublishSubject.create();
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    publishSubject.onNext(50000 + new Random().nextInt(10000) / 10000d);
                }
            }, 0, 1000);
        }
    }

    public class MockNetworkService {
        public MockNetworkService() {
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    int i1 = new Random().nextInt(2);
                    if (i1 == 1) {
                        for (int i = 0; i < new Random().nextInt(5); i++) {
                            addOffer();
                        }
                    } else {
                        int i2 = new Random().nextInt(3);
                        for (int i = 0; i < i2; i++) {
                            removeOffer();
                        }
                    }

                }
            }, 0, 500);
        }
    }
}
