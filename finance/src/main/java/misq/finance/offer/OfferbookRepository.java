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

package misq.finance.offer;

import io.reactivex.rxjava3.subjects.PublishSubject;
import lombok.extern.slf4j.Slf4j;
import misq.p2p.MockNetworkService;
import misq.p2p.NetworkService;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class OfferbookRepository {
    private final List<Offer> offers = new CopyOnWriteArrayList<>();
    protected final PublishSubject<Offer> offerAddedSubject;
    protected final PublishSubject<Offer> offerRemovedSubject;

    public OfferbookRepository(NetworkService networkService) {
        offerAddedSubject = PublishSubject.create();
        offerRemovedSubject = PublishSubject.create();

        offers.addAll(MockOfferBuilder.makeOffers().values());

        networkService.addListener(new MockNetworkService.Listener() {
            @Override
            public void onDataAdded(Serializable serializable) {
                if (serializable instanceof Offer) {
                    Offer offer = (Offer) serializable;
                    offers.add(offer);
                    offerAddedSubject.onNext(offer);
                }
            }

            @Override
            public void onDataRemoved(Serializable serializable) {
                if (serializable instanceof Offer) {
                    Offer offer = (Offer) serializable;
                    offers.remove(offer);
                    offerRemovedSubject.onNext(offer);
                }

            }
        });
    }

    public void initialize() {
    }

    public List<Offer> getOffers() {
        return offers;
    }

    public PublishSubject<Offer> getOfferAddedSubject() {
        return offerAddedSubject;
    }

    public PublishSubject<Offer> getOfferRemovedSubject() {
        return offerRemovedSubject;
    }
}
