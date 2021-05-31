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
import misq.finance.swap.offer.SwapOffer;
import misq.presentation.formatters.AmountFormatter;
import misq.presentation.formatters.DateFormatter;

import java.text.DecimalFormat;
import java.util.*;

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
        String minAmountString = minAmountAsPercentage
                .map(e -> Math.round(amount * e))
                .map(e -> AmountFormatter.formatAmount(e, currencyCode) + " - ")
                .orElse("");
        return minAmountString + formatAmount(amount, currencyCode);
    }

    public static String formatAmount(long amount, String currencyCode) {
        return AmountFormatter.formatAmount(amount, currencyCode);
    }

    public static long getSmallestBaseAmount(List<SwapOffer> offers) {
        return offers.stream()
                .mapToLong(SwapOffer::getMinBaseAmount)
                .min()
                .orElse(0);
    }

    public static long getLargestBaseAmount(List<SwapOffer> offers) {
        return offers.stream()
                .mapToLong(offer -> offer.getBaseAsset().getAmount())
                .max()
                .orElse(0);
    }

    public static long getSmallestQuoteAmount(List<SwapOffer> offers, Double marketPrice) {
        return offers.stream()
                .mapToLong(offer -> getMinQuoteAmount(offer.getBaseAsset().getAmount(), offer.getMinAmountAsPercentage(), offer.getMarketBasedPrice(), marketPrice))
                .min()
                .orElse(0);
    }

    public static long getLargestQuoteAmount(List<SwapOffer> offers, Double marketPrice) {
        return offers.stream()
                .mapToLong(offer -> getQuoteAmount(offer.getBaseAsset().getAmount(), offer.getMarketBasedPrice(), marketPrice))
                .peek(e -> log.error("" + e)).max()
                .orElse(0);
    }

    public static double getLowestPrice(List<SwapOffer> offers, Optional<Double> marketPrice) {
        return offers.stream()
                .mapToDouble(offer -> getPrice(offer.getFixPrice(), offer.getMarketBasedPrice(), marketPrice))
                .min()
                .orElse(0);
    }

    public static double getHighestPrice(List<SwapOffer> offers, Optional<Double> marketPrice) {
        return offers.stream()
                .mapToDouble(offer -> getPrice(offer.getFixPrice(), offer.getMarketBasedPrice(), marketPrice))
                .max()
                .orElse(0);
    }

    public static String formatTransferTypes(List<TransferType> transferTypes) {
        return transferTypes.toString();
    }

    public static String formatAssetTransferType(AssetTransfer.Type assetTransferType) {
        return assetTransferType.toString();
    }

    public static Map.Entry<String, Double> getPriceTuple(double fixPrice, Optional<Double> marketBasedPrice, double marketPrice) {
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
        String displayString = df.format(price) + " (" + df.format(percentage * 100) + "%)";
        return new AbstractMap.SimpleEntry<>(displayString, price);
    }

    public static double getPrice(double fixPrice, Optional<Double> marketBasedPrice, Optional<Double> marketPrice) {
        return marketBasedPrice.map(percentage -> marketPrice
                .map(marketPrice1 -> marketPrice1 * (1 + percentage)).orElse(fixPrice))
                .orElse(fixPrice);
    }

    public static String getFormattedQuoteAmount(long baseAmount, Optional<Double> minAmountAsPercentage, Optional<Double> marketBasedPrice, double marketPrice, String currencyCode) {
        double amount = getQuoteAmount(baseAmount, marketBasedPrice, marketPrice) / 10000d;
        String minAmountString;
        DecimalFormat df = new DecimalFormat("#.##");
        df.setMinimumFractionDigits(2);
        if (minAmountAsPercentage.isPresent()) {
            long minAmount = Math.round(amount * minAmountAsPercentage.get());
            minAmountString = df.format(minAmount) + " - ";
        } else {
            minAmountString = "";
        }
        return minAmountString + df.format(amount) + " " + currencyCode;
    }

    public static long getQuoteAmount(long baseAmount, Optional<Double> marketBasedPrice, double marketPrice) {
        double amount;
        double percentage;
        double price;

        if (marketBasedPrice.isPresent()) {
            percentage = marketBasedPrice.get();
            price = marketPrice * (1 + percentage);
            amount = baseAmount / 100000000d * price;
        } else {
            amount = baseAmount / 100000000d;
        }
        return Math.round(amount * 10000);
    }

    public static long getMinQuoteAmount(long baseAmount, Optional<Double> minAmountAsPercentage, Optional<Double> marketBasedPrice, double marketPrice) {
        double amount;
        double percentage;
        double price;

        if (marketBasedPrice.isPresent()) {
            percentage = marketBasedPrice.get();
            price = marketPrice * (1 + percentage);
            amount = baseAmount / 100000000d * price;
        } else {
            amount = baseAmount / 100000000d;
        }

        if (minAmountAsPercentage.isPresent()) {
            long minAmount = Math.round(amount * minAmountAsPercentage.get());
            return Math.round(minAmount * 10000);
        } else {
            return Math.round(amount * 10000);
        }
    }

    public static int comparePrice(Double selfPrice, Double otherPrice, String bidAssetCode, String quoteCurrencyCode) {
        if (bidAssetCode.equals(quoteCurrencyCode)) {
            return Double.compare(otherPrice, selfPrice);
        } else {
            return Double.compare(selfPrice, otherPrice);
        }
    }
}
