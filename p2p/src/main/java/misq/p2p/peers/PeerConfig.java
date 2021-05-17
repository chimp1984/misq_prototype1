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

package misq.p2p.peers;

import lombok.Getter;
import misq.p2p.Address;
import misq.p2p.peers.exchange.PeerExchangeConfig;

import java.util.List;

@Getter
public class PeerConfig {
    private final PeerExchangeConfig peerExchangeConfig;
    private final List<Address> seedNodes;
    private final int minNumConnectedPeers;
    private final int maxNumConnectedPeers;
    private final int minNumReportedPeers;

    public PeerConfig(PeerExchangeConfig peerExchangeConfig, List<Address> seedNodes) {
        this(peerExchangeConfig, seedNodes, 8, 12, 1);
    }

    public PeerConfig(PeerExchangeConfig peerExchangeConfig,
                      List<Address> seedNodes,
                      int minNumConnectedPeers,
                      int maxNumConnectedPeers,
                      int minNumReportedPeers) {
        this.peerExchangeConfig = peerExchangeConfig;
        this.seedNodes = seedNodes;
        this.minNumConnectedPeers = minNumConnectedPeers;
        this.maxNumConnectedPeers = maxNumConnectedPeers;
        this.minNumReportedPeers = minNumReportedPeers;
    }
}
