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

import io.reactivex.rxjava3.subjects.PublishSubject;
import lombok.extern.slf4j.Slf4j;
import misq.finance.offer.Offer;
import misq.finance.offer.OfferbookRepository;
import misq.finance.swap.offer.SwapOffer;
import misq.marketprice.MarketPriceService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
public class OfferbookEntity implements OfferbookRepository.Listener {
    protected final MarketPriceService marketPriceService;
    protected final OfferbookRepository offerbookRepository;
    protected final List<OfferEntity> offerEntities = new CopyOnWriteArrayList<>();
    protected final PublishSubject<OfferEntity> offerEntityAddedSubject;
    protected final PublishSubject<OfferEntity> offerEntityRemovedSubject;
    // protected final BehaviorSubject<Double> marketPriceSubject;
    private double marketPrice;

    public OfferbookEntity(OfferbookRepository offerbookRepository, MarketPriceService marketPriceService) {
        this.offerbookRepository = offerbookRepository;
        this.marketPriceService = marketPriceService;

        offerEntityAddedSubject = PublishSubject.create();
        offerEntityRemovedSubject = PublishSubject.create();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void initialize() {
    }

    public void activate() {
        offerbookRepository.addListener(this);

        offerEntities.addAll(offerbookRepository.getOffers().stream()
                .map(offer -> new OfferEntity((SwapOffer) offer, marketPriceService.getMarketPriceSubject()))
                .collect(Collectors.toList()));

        this.marketPrice = marketPriceService.getMarketPrice();
    }

    public void deactivate() {
        offerbookRepository.removeListener(this);
    }

    public List<OfferEntity> getOfferEntities() {
        return offerEntities;
    }

    public PublishSubject<OfferEntity> getOfferEntityAddedSubject() {
        return offerEntityAddedSubject;
    }

    public PublishSubject<OfferEntity> getOfferEntityRemovedSubject() {
        return offerEntityRemovedSubject;
    }



    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Internal
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onOfferAdded(Offer offer) {
        if (offer instanceof SwapOffer) {
            OfferEntity offerEntity = new OfferEntity((SwapOffer) offer, marketPriceService.getMarketPriceSubject());
            offerEntities.add(offerEntity);
            offerEntityAddedSubject.onNext(offerEntity);
        }
    }

    @Override
    public void onOfferRemoved(Offer offer) {
        offerEntities.stream()
                .filter(e -> e.getOffer().equals(offer))
                .findAny()
                .ifPresent(offerEntity -> {
                    offerEntities.remove(offerEntity);
                    offerEntityRemovedSubject.onNext(offerEntity);
                });
    }
}
