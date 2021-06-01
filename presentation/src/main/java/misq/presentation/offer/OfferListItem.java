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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import misq.common.util.Tuple2;
import misq.finance.swap.offer.SwapOffer;
import org.jetbrains.annotations.NotNull;

public class OfferListItem implements Comparable<OfferListItem> {
    @Getter
    private final SwapOffer offer;
    private final DoubleProperty marketPrice;
    @Getter
    private final String baseAmountWithMinAmount;
    @Getter
    private final String transferOptions;
    private final ChangeListener<Number> listener;
    private final DoubleProperty priceAsDouble = new SimpleDoubleProperty(0d);
    @Getter
    private final StringProperty price = new SimpleStringProperty("");
    @Getter
    private final StringProperty quoteAmount = new SimpleStringProperty("");

    public OfferListItem(SwapOffer offer, DoubleProperty marketPrice) {
        this.offer = offer;
        this.marketPrice = marketPrice;

        baseAmountWithMinAmount = OfferFormatter.formatAmountWithMinAmount(offer.getBaseAsset().getAmount(),
                offer.getMinAmountAsPercentage(),
                offer.getBaseCurrency());
        transferOptions = OfferFormatter.formatTransferOptions(offer.getTransferOptions());

        listener = (observable, oldValue, newValue) -> updatedPriceAndAmount();
        updatedPriceAndAmount();
    }

    private void updatedPriceAndAmount() {
        Tuple2<String, Double> priceTuple = OfferbookModel.getPriceTuple(offer.getFixPrice(),
                offer.getMarketBasedPrice(),
                marketPrice.get());
        price.set(priceTuple.first);
        priceAsDouble.set(priceTuple.second);
        quoteAmount.set(OfferbookModel.getFormattedQuoteAmount(offer.getBaseAsset().getAmount(),
                offer.getMinAmountAsPercentage(),
                offer.getMarketBasedPrice(),
                marketPrice.get(),
                offer.getQuoteAsset().getCode()));
    }

    public void isVisible(boolean visible) {
        if (visible) {
            updatedPriceAndAmount();
            marketPrice.addListener(listener);
        } else {
            marketPrice.removeListener(listener);
        }
    }

    public int compareBaseAmount(OfferListItem other) {
        return Long.compare(offer.getBaseAsset().getAmount(), other.getOffer().getBaseAsset().getAmount());
    }

    public int compareQuoteAmount(OfferListItem other) {
        return Long.compare(offer.getQuoteAsset().getAmount(), other.getOffer().getQuoteAsset().getAmount());
    }

    public int comparePrice(OfferListItem other) {
        return Double.compare(priceAsDouble.get(), other.priceAsDouble.get());
    }

    @Override
    public int compareTo(@NotNull OfferListItem other) {
        return comparePrice(other);
    }
}
