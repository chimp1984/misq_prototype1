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

package misq.finance.swap.offer;

import lombok.Getter;
import misq.finance.Asset;
import misq.finance.offer.*;
import misq.finance.swap.SwapProtocolType;
import misq.p2p.NetworkId;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Offer for a asset swap offer. Supports multiple protocolTypes in case the maker wants to give more flexibility
 * to takers.
 */
@Getter
public class SwapOffer extends Offer {
    private final Asset bidAsset;
    private final Asset askAsset;
    private final String baseCurrency;
    private final Optional<Double> marketBasedPrice;
    private final Optional<Double> minAmountAsPercentage;

    private transient final long minBaseAmount;

    public SwapOffer(List<SwapProtocolType> protocolTypes,
                     NetworkId makerNetworkId,
                     Asset bidAsset,
                     Asset askAsset) {
        this(bidAsset, askAsset, bidAsset.getCode(), protocolTypes, makerNetworkId,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    public SwapOffer(Asset bidAsset,
                     Asset askAsset,
                     String baseCurrency,
                     List<SwapProtocolType> protocolTypes,
                     NetworkId makerNetworkId,
                     Optional<Double> marketBasedPrice,
                     Optional<Double> minAmountAsPercentage,
                     Optional<DisputeResolutionOptions> disputeResolutionOptions,
                     Optional<FeeOptions> feeOptions,
                     Optional<ReputationOptions> reputationOptions,
                     Optional<TransferOptions> transferOptions) {
        super(protocolTypes, makerNetworkId, disputeResolutionOptions, feeOptions, reputationOptions, transferOptions);

        this.bidAsset = bidAsset;
        this.askAsset = askAsset;
        this.baseCurrency = baseCurrency;
        this.marketBasedPrice = marketBasedPrice;
        this.minAmountAsPercentage = minAmountAsPercentage;

        minBaseAmount = minAmountAsPercentage.map(perc -> Math.round(getBaseAsset().getAmount() * perc))
                .orElse(getBaseAsset().getAmount());
    }

    public double getFixPrice() {
        double baseAssetAmount = (double) getBaseAsset().getAmount();
        double quoteAssetAmount = (double) getQuoteAsset().getAmount();
        checkArgument(quoteAssetAmount > 0);
        return quoteAssetAmount / baseAssetAmount * 10000; // for fiat...
    }


    public Asset getBaseAsset() {
        if (askAsset.getCode().equals(baseCurrency)) {
            return askAsset;
        } else {
            return bidAsset;
        }
    }

    public Asset getQuoteAsset() {
        if (bidAsset.getCode().equals(baseCurrency)) {
            return askAsset;
        } else {
            return bidAsset;
        }
    }
}
