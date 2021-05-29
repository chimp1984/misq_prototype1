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

package misq.desktop;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.extern.slf4j.Slf4j;
import misq.finance.Asset;
import misq.finance.offer.Offer;
import misq.finance.swap.offer.SwapOffer;
import misq.jfx.common.LifeCycleChangeListener;
import misq.jfx.common.ViewModel;
import misq.jfx.main.content.offerbook.OfferListItem;
import misq.jfx.main.content.offerbook.OfferbookViewModel;
import misq.jfx.utils.UserThread;
import misq.presentation.Offerbook;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class OfferbookDataModel implements LifeCycleChangeListener {
    private final Offerbook offerbook;
    private final DoubleProperty marketPrice = new SimpleDoubleProperty();
    private final Observer<Double> marketPriceObserver;
    private final Consumer<Offer> addedOffersConsumer;
    private final Consumer<Offer> removedOffersConsumer;
    private OfferbookViewModel viewModel;

    public OfferbookDataModel(Offerbook offerbook) {
        this.offerbook = offerbook;

        addedOffersConsumer = offer -> viewModel.onOfferListItemAdded(toOfferListItem(offer));
        removedOffersConsumer = offer -> viewModel.onOfferListItemRemoved(offer.getId());
        marketPriceObserver = new Observer<>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
            }

            @Override
            public void onNext(@NonNull Double value) {
                marketPrice.set(value);
            }

            @Override
            public void onError(@NonNull Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        };
    }

    @Override
    public void onConstructView(ViewModel viewModel) {
        this.viewModel = (OfferbookViewModel) viewModel;
    }

    @Override
    public void onViewAdded() {
        List<OfferListItem> listItems = offerbook.offers.stream().map(this::toOfferListItem).collect(Collectors.toList());
        viewModel.setOfferListItems(listItems);
        offerbook.marketPriceService.publishSubject.subscribe(marketPriceObserver);
        offerbook.addAddedOffersConsumer(addedOffersConsumer);
        offerbook.addRemovedOffersConsumer(removedOffersConsumer);
    }

    @Override
    public void onViewRemoved() {
        offerbook.marketPriceService.publishSubject.onComplete();
        offerbook.removeAddedOffersConsumer(addedOffersConsumer);
        offerbook.removeRemovedOffersConsumer(removedOffersConsumer);
    }

    private OfferListItem toOfferListItem(Offer offer) {
        SwapOffer swapOffer = (SwapOffer) offer;
        String maker = swapOffer.getMakerAddress().toString();
        Asset askAsset = swapOffer.getAskAsset();
        String amount = String.valueOf(askAsset.getAmount() / 1000000d).substring(0, 3);
        String details = "Zelle/Multisig";

        StringProperty price = new SimpleStringProperty("");
        UserThread.runPeriodically(() -> {
            double rand = new Random().nextInt(10000) / 10000d;
            double marketPrice = 50000 + 1000 * rand;
            double percentageBasedPrice = 0.02;
            double percentagePrice = marketPrice * (1 + percentageBasedPrice);
            DecimalFormat df = new DecimalFormat("#.##");
            String formatted = df.format(percentagePrice);
            price.set(formatted);
        }, 1000);

        return new OfferListItem(swapOffer.getId(), 0.02, amount, price, maker, details, marketPrice, offerbook::getFormattedMarketBasedPrice);
    }
}
