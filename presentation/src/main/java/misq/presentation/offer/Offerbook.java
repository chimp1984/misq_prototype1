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

package misq.presentation.offer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.finance.offer.Offer;
import misq.presentation.offer.mock.MockNetworkService;
import misq.presentation.offer.mock.NetworkService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class Offerbook {
    public interface Listener {
        void onOfferAdded(Offer offer);

        void onOfferRemoved(Offer offer);
    }

    @Getter
    private final List<Offer> offers = new CopyOnWriteArrayList<>();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    private final NetworkService networkService;

    public Offerbook(NetworkService networkService) {
        this.networkService = networkService;

        offers.addAll(networkService.getData().values());

        networkService.addListener(new MockNetworkService.Listener() {
            @Override
            public void onOfferAdded(Offer offer) {
                offers.add(offer);
                listeners.forEach(listener -> listener.onOfferAdded(offer));
            }

            @Override
            public void onOfferRemoved(Offer offer) {
                offers.remove(offer);
                listeners.forEach(listener -> listener.onOfferRemoved(offer));
            }
        });
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
