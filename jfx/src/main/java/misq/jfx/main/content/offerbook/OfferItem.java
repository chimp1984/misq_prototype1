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

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Getter
public class OfferItem {
    private final String id;
    private final String date;
    private final String protocolTypes;
    private final String makerInfo;     // If maker provides info shows at avatar
    private final String reputationOptions;
    private final String transferOptions;

    private final String bidAssetCode;
    private final String bidAssetAmount;  // includes minAmount if set
    private final String bidAssetTransferTypes; // E.g. Native chain, host chain, fiat method (zelle, sepa)
    private final String bidAssetAssetTransferType; // In case of crypto it can be automatic or manual. Fiat usually is always manual.

    private final String askAssetCode;
    private final String askAssetAmount;    // includes minAmount if set
    private final String askAssetTransferTypes; // E.g. Native chain, host chain, fiat method (zelle, sepa)
    private final String askAssetAssetTransferType; // In case of crypto it can be automatic or manual. Fiat usually is always manual.


    private final StringProperty price = new SimpleStringProperty("");     // derived from market price and marketBasedPrice value if marketBasedPrice is set, otherwise derived from ask/bid amounts
    private final StringProperty quoteAmount = new SimpleStringProperty("");     // derived from market price and marketBasedPrice value if marketBasedPrice is set, otherwise derived from ask/bid amounts

    private final double fixPriceAsDouble;
    private final Optional<Double> marketBasedPrice;       // provided for calculating the market based price
    private final DoubleProperty marketPrice;       // used for getting notified for market price updates to call priceSupplied function
    private final QuoteAmountSupplier quoteAmountSupplier;
    private final String quoteCurrencyCode;
    private final long baseAmountAsLong;
    private final String baseAmountWithMinAmount;
    private final Optional<Double> minAmountAsPercentage;
    private final PriceSupplier priceSupplier;

    private final ChangeListener<Number> listener;

    public OfferItem(String id,
                     String date,
                     String protocolTypes,
                     String makerInfo,
                     String reputationOptions,
                     String transferOptions,
                     String bidAssetCode,
                     String bidAssetAmount,
                     String bidAssetTransferTypes,
                     String bidAssetAssetTransferType,
                     String askAssetCode,
                     String askAssetAmount,
                     String askAssetTransferTypes,
                     String askAssetAssetTransferType,
                     double fixPriceAsDouble,
                     Optional<Double> marketBasedPrice,
                     DoubleProperty marketPrice,
                     PriceSupplier priceSupplier,
                     QuoteAmountSupplier quoteAmountSupplier,
                     String quoteCurrencyCode,
                     long baseAmountAsLong,
                     String baseAmountWithMinAmount,
                     Optional<Double> minAmountAsPercentage) {
        this.id = id;
        this.date = date;
        this.protocolTypes = protocolTypes;
        this.makerInfo = makerInfo;
        this.reputationOptions = reputationOptions;
        this.transferOptions = transferOptions;
        this.bidAssetCode = bidAssetCode;
        this.bidAssetAmount = bidAssetAmount;
        this.bidAssetTransferTypes = bidAssetTransferTypes;
        this.bidAssetAssetTransferType = bidAssetAssetTransferType;
        this.askAssetCode = askAssetCode;
        this.askAssetAmount = askAssetAmount;
        this.askAssetTransferTypes = askAssetTransferTypes;
        this.askAssetAssetTransferType = askAssetAssetTransferType;
        this.fixPriceAsDouble = fixPriceAsDouble;
        this.marketBasedPrice = marketBasedPrice;
        this.marketPrice = marketPrice;
        this.quoteAmountSupplier = quoteAmountSupplier;
        this.quoteCurrencyCode = quoteCurrencyCode;
        this.baseAmountAsLong = baseAmountAsLong;
        this.baseAmountWithMinAmount = baseAmountWithMinAmount;
        this.minAmountAsPercentage = minAmountAsPercentage;

        // We get called from a non UI thread so we need to wrap it into Platform.runLater
        listener = (observable, oldValue, newValue) -> Platform.runLater(this::updatedPriceAndAmount);
        this.priceSupplier = priceSupplier;
        updatedPriceAndAmount();
    }

    private void updatedPriceAndAmount() {
        price.set(priceSupplier.get(fixPriceAsDouble, marketBasedPrice, marketPrice.get()));
        quoteAmount.set(quoteAmountSupplier.get(baseAmountAsLong, minAmountAsPercentage, marketBasedPrice, marketPrice.get(), quoteCurrencyCode));
    }

    public void isVisible(boolean visible) {
        if (visible) {
            updatedPriceAndAmount();
            marketPrice.addListener(listener);
        } else {
            marketPrice.removeListener(listener);
        }
    }
}
