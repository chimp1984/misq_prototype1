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

package misq.desktop.offer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import lombok.extern.slf4j.Slf4j;
import misq.finance.Asset;
import misq.finance.offer.Offer;
import misq.finance.swap.offer.SwapOffer;
import misq.jfx.common.LifeCycleChangeListener;
import misq.jfx.common.ViewModel;
import misq.jfx.main.content.offerbook.OfferItem;
import misq.jfx.main.content.offerbook.OfferbookViewModel;
import misq.jfx.main.content.offerbook.PriceSupplier;
import misq.jfx.main.content.offerbook.QuoteAmountSupplier;
import misq.presentation.marketprice.MarketPriceService;
import misq.presentation.marketprice.MockMarketPriceService;
import misq.presentation.offer.OfferDisplay;
import misq.presentation.offer.Offerbook;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class OfferbookDataModel implements LifeCycleChangeListener {
    private final Offerbook offerbook;
    private final MarketPriceService marketPriceService;
    private final DoubleProperty marketPrice = new SimpleDoubleProperty();
    private final Offerbook.Listener offerBookListener;
    private final MockMarketPriceService.Listener marketPriceListener;
    private OfferbookViewModel viewModel;

    public OfferbookDataModel(Offerbook offerbook, MarketPriceService marketPriceService) {
        this.offerbook = offerbook;
        this.marketPriceService = marketPriceService;

        offerBookListener = new Offerbook.Listener() {
            @Override
            public void onOfferAdded(Offer offer) {
                if (offer instanceof SwapOffer) {
                    OfferItem offerItem = toOfferItem((SwapOffer) offer);
                    viewModel.onOfferItemAdded(offerItem);
                }
            }

            @Override
            public void onOfferRemoved(Offer offer) {
                viewModel.onOfferListItemRemoved(offer.getId());
            }
        };

        // marketPriceListener = marketPrice::set;
        marketPriceListener = new MockMarketPriceService.Listener() {
            @Override
            public void onPriceUpdate(double value) {
                marketPrice.set(value);
            }
        };
    }

    @Override
    public void onConstructView(ViewModel viewModel) {
        this.viewModel = (OfferbookViewModel) viewModel;
    }

    @Override
    public void onViewAdded() {
        marketPrice.set(marketPriceService.getMarketPrice());
        marketPriceService.addListener(marketPriceListener);

        List<OfferItem> items = offerbook.getOffers().stream()
                .filter(e -> e instanceof SwapOffer)
                .map(e -> (SwapOffer) e)
                .map(this::toOfferItem)
                .collect(Collectors.toList());
        viewModel.setOfferItems(items);
        offerbook.addListener(offerBookListener);
    }

    @Override
    public void onViewRemoved() {
        marketPriceService.removeListener(marketPriceListener);
        offerbook.removeListener(offerBookListener);
    }

    private OfferItem toOfferItem(SwapOffer offer) {
        String id = offer.getId();
        String date = OfferDisplay.formatDate(offer.getDate());
        String protocolTypes = OfferDisplay.formatProtocolTypes(offer.getProtocolTypes());
        String makerInfo = "makerInfo";
        String reputationOptions = OfferDisplay.formatReputationOptions(offer.getReputationOptions());
        String transferOptions = OfferDisplay.formatTransferOptions(offer.getTransferOptions());

        Asset bidAsset = offer.getBidAsset();
        String bidAssetCode = bidAsset.getCode();
        String bidAssetAmount = "";//OfferDisplay.formatAmount(bidAsset.getAmount(), bidAssetCode);
        String bidAssetTransferTypes = OfferDisplay.formatTransferTypes(bidAsset.getTransferTypes());
        String bidAssetAssetTransferType = OfferDisplay.formatAssetTransferType(bidAsset.getAssetTransferType());

        Asset askAsset = offer.getAskAsset();
        String askAssetCode = askAsset.getCode();
        String askAssetAmount = "";// OfferDisplay.formatAmount(askAsset.getAmount(), askAssetCode);
        String askAssetTransferTypes = OfferDisplay.formatTransferTypes(askAsset.getTransferTypes());
        String askAssetAssetTransferType = OfferDisplay.formatAssetTransferType(askAsset.getAssetTransferType());

        double fixPriceAsDouble = offer.getPrice();

        Optional<Double> marketBasedPrice = offer.getMarketBasedPrice();
        PriceSupplier priceSupplier = OfferDisplay::getPrice;
        QuoteAmountSupplier quoteAmountSupplier = OfferDisplay::getQuoteAmount;
        long baseAmountAsLong = offer.getBaseAsset().getAmount();
        String baseAmountWithMinAmount = OfferDisplay.formatBaseAmount(offer.getBaseAsset().getAmount(), offer.getMinAmountAsPercentage(), offer.getBaseAsset().getCode());
        String quoteCurrencyCode = offer.getQuoteAsset().getCode();
        return new OfferItem(id,
                date,
                protocolTypes,
                makerInfo,
                reputationOptions,
                transferOptions,
                bidAssetCode,
                bidAssetAmount,
                bidAssetTransferTypes,
                bidAssetAssetTransferType,
                askAssetCode,
                askAssetAmount,
                askAssetTransferTypes,
                askAssetAssetTransferType,
                fixPriceAsDouble,
                marketBasedPrice,
                marketPrice,
                priceSupplier,
                quoteAmountSupplier,
                quoteCurrencyCode,
                baseAmountAsLong,
                baseAmountWithMinAmount,
                offer.getMinAmountAsPercentage());
    }
}
