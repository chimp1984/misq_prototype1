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

import lombok.extern.slf4j.Slf4j;
import misq.account.TransferType;
import misq.finance.ProtocolType;
import misq.finance.contract.AssetTransfer;
import misq.finance.offer.ReputationOptions;
import misq.finance.offer.TransferOptions;
import misq.presentation.formatters.AmountFormatter;
import misq.presentation.formatters.DateFormatter;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
public class OfferDisplay {
    public static String formatDate(long date) {
        return DateFormatter.formatDateTime(new Date(date));
    }

    public static String formatProtocolTypes(List<? extends ProtocolType> protocolTypes) {
        return protocolTypes.toString();
    }

    public static String formatReputationOptions(Optional<ReputationOptions> reputationOptions) {
        return reputationOptions.toString();
    }

    public static String formatTransferOptions(Optional<TransferOptions> transferOptions) {
        return transferOptions.map(e -> e.getBankName() + " / " + e.getCountyCodeOfBank()).orElse("-");
    }

    public static String formatAmountWithMinAmount(long amount, Optional<Double> minAmountAsPercentage, String currencyCode) {
        String amountString = AmountFormatter.formatAmount(amount, currencyCode);
        String minAmountString = minAmountAsPercentage
                .map(e -> Math.round(amount * e))
                .map(e -> AmountFormatter.formatAmount(e, currencyCode) + " - ")
                .orElse("");
        return minAmountString + amountString;
    }

    public static String formatTransferTypes(List<TransferType> transferTypes) {
        return transferTypes.toString();
    }

    public static String formatAssetTransferType(AssetTransfer.Type assetTransferType) {
        return assetTransferType.toString();
    }

    public static String getPrice(double fixPrice, Optional<Double> marketBasedPrice, double marketPrice) {
        double percentage;
        double price;
        DecimalFormat df = new DecimalFormat("#.##");
        df.setMinimumFractionDigits(2);
        if (marketBasedPrice.isPresent()) {
            percentage = marketBasedPrice.get();
            price = marketPrice * (1 + percentage);
        } else {
            percentage = marketPrice / fixPrice;
            price = fixPrice;
        }
        return df.format(price) + " (" + df.format(percentage * 100) + "%)";
    }

    public static String getQuoteAmount(long baseAmount, Optional<Double> minAmountAsPercentage, Optional<Double> marketBasedPrice, double marketPrice) {
        double amount;
        double percentage;
        double price;
        DecimalFormat df = new DecimalFormat("#.##");
        df.setMinimumFractionDigits(2);
        if (marketBasedPrice.isPresent()) {
            percentage = marketBasedPrice.get();
            price = marketPrice * (1 + percentage);
            amount = baseAmount / 100000000d * price;
        } else {
            amount = baseAmount / 100000000d;
        }

        String minAmountString;
        if (minAmountAsPercentage.isPresent()) {
            long minAmount = Math.round(amount * minAmountAsPercentage.get());
            minAmountString = df.format(minAmount) + " - ";
        } else {
            minAmountString = "";
        }
        return minAmountString + df.format(amount);
    }

}
