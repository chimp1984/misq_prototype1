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

import java.util.Map;

public class ContractMaker {
    public static TwoPartyContract createMakerTrade(Address takerAddress, ProtocolType protocolType) {
        Party counterParty = new Party(takerAddress);
        return new TwoPartyContract(protocolType, Role.MAKER, counterParty);
    }

    public static TwoPartyContract createTakerTrade(Offer offer, ProtocolType protocolType) {
        Address makerAddress = offer.getMakerAddress();
        Party counterParty = new Party(makerAddress);
        return new TwoPartyContract(protocolType, Role.TAKER, counterParty);
    }

    public static ManyPartyContract createMakerTrade(Address takerAddress, Address escrowAgentAddress, ProtocolType protocolType) {
        Party taker = new Party(takerAddress);
        Party escrowAgent = new Party(escrowAgentAddress);
        return new ManyPartyContract(protocolType, Role.MAKER, Map.of(Role.MAKER, self(), Role.TAKER, taker, Role.ESCROW_AGENT, escrowAgent));
    }

    public static ManyPartyContract createTakerTrade(Offer offer, Address escrowAgentAddress, ProtocolType protocolType) {
        Address makerAddress = offer.getMakerAddress();
        Party maker = new Party(makerAddress);
        Party escrowAgent = new Party(escrowAgentAddress);
        return new ManyPartyContract(protocolType, Role.TAKER, Map.of(Role.MAKER, maker, Role.TAKER, self(), Role.ESCROW_AGENT, escrowAgent));
    }

    private static Party self() {
        return new Party(Address.localHost(1000));
    }
}
