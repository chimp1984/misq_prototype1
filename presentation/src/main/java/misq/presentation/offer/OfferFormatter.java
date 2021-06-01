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

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
public class OfferFormatter {
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

    public static String formatTransferTypes(List<TransferType> transferTypes) {
        return transferTypes.toString();
    }

    public static String formatAssetTransferType(AssetTransfer.Type assetTransferType) {
        return assetTransferType.toString();
    }
}
