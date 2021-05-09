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
import misq.finance.offer.Offer;
import misq.finance.swap.SwapProtocolType;
import misq.p2p.node.Address;

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
    private final Optional<Double> marketBasedPrice;

    public SwapOffer(List<SwapProtocolType> protocolTypes,
                     Address makerAddress,
                     Asset bidAsset,
                     Asset askAsset,
                     Optional<Double> marketBasedPrice) {
        super(protocolTypes, makerAddress);

        this.bidAsset = bidAsset;
        this.askAsset = askAsset;
        this.marketBasedPrice = marketBasedPrice;
    }

    public double getPrice() {
        return marketBasedPrice.orElse(getFixPrice());
    }

    private double getFixPrice() {
        double baseAssetAmount = (double) getBaseAsset().getAmount();
        double counterAssetAmount = (double) getCounterAsset().getAmount();
        checkArgument(counterAssetAmount > 0);
        return baseAssetAmount / counterAssetAmount;
    }

    private Asset getBaseAsset() {
        if (askAsset.isBase()) {
            return askAsset;
        } else {
            return bidAsset;
        }
    }

    private Asset getCounterAsset() {
        if (bidAsset.isBase()) {
            return askAsset;
        } else {
            return bidAsset;
        }
    }
}
