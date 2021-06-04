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

package misq.finance.offer;

import misq.account.FiatTransferType;
import misq.finance.Asset;
import misq.finance.swap.SwapProtocolType;
import misq.finance.swap.offer.SwapOffer;
import misq.p2p.Address;
import misq.p2p.NetworkId;
import misq.p2p.NetworkService;

import java.util.List;

public class OpenOffers {
    private final NetworkService networkService;

    public OpenOffers(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void createNewOffer(long askAmount) {
        NetworkId makerNetworkId = new NetworkId(Address.localHost(3333), null, "default");
        Asset askAsset = new Asset("BTC", askAmount, List.of());
        Asset bidAsset = new Asset("USD", 5000, List.of(FiatTransferType.ZELLE));
        SwapOffer offer = new SwapOffer(List.of(SwapProtocolType.REPUTATION, SwapProtocolType.MULTISIG),
                makerNetworkId, bidAsset, askAsset);
        networkService.addData(offer);
    }
}
