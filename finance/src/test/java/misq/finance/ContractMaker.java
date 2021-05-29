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

package misq.finance;

import misq.finance.contract.ManyPartyContract;
import misq.finance.contract.Party;
import misq.finance.contract.TwoPartyContract;
import misq.finance.offer.Offer;
import misq.p2p.Address;
import misq.p2p.NetworkId;

import java.util.Map;

public class ContractMaker {
    public static TwoPartyContract createMakerTrade(NetworkId takerNetworkId, ProtocolType protocolType) {
        Party counterParty = new Party(takerNetworkId);
        return new TwoPartyContract(protocolType, Role.MAKER, counterParty);
    }

    public static TwoPartyContract createTakerTrade(Offer offer, ProtocolType protocolType) {
        NetworkId makerNetworkId = offer.getMakerNetworkId();
        Party counterParty = new Party(makerNetworkId);
        return new TwoPartyContract(protocolType, Role.TAKER, counterParty);
    }

    public static ManyPartyContract createMakerTrade(NetworkId takerNetworkId, NetworkId escrowAgentNetworkId, ProtocolType protocolType) {
        Party taker = new Party(takerNetworkId);
        Party escrowAgent = new Party(escrowAgentNetworkId);
        return new ManyPartyContract(protocolType, Role.MAKER, Map.of(Role.MAKER, self(), Role.TAKER, taker, Role.ESCROW_AGENT, escrowAgent));
    }

    public static ManyPartyContract createTakerTrade(Offer offer, NetworkId escrowAgentNetworkId, ProtocolType protocolType) {
        NetworkId makerNetworkId = offer.getMakerNetworkId();
        Party maker = new Party(makerNetworkId);
        Party escrowAgent = new Party(escrowAgentNetworkId);
        return new ManyPartyContract(protocolType, Role.TAKER, Map.of(Role.MAKER, maker, Role.TAKER, self(), Role.ESCROW_AGENT, escrowAgent));
    }

    private static Party self() {
        return new Party(new NetworkId(Address.localHost(1000), null, "default"));
    }
}
