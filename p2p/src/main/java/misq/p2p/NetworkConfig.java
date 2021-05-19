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

package misq.p2p;

import lombok.Getter;
import misq.p2p.peers.PeerConfig;
import misq.p2p.peers.exchange.PeerExchangeConfig;

@Getter
public class NetworkConfig {
    private final NetworkType networkType;
    private final PeerConfig peerConfig;
    private final String baseDirPath;
    private final NodeId nodeId;

    public NetworkConfig(String baseDirPath, NodeId nodeId, NetworkType networkType) {
        this(baseDirPath,
                nodeId,
                networkType,
                new PeerConfig(new PeerExchangeConfig(), new SeedNodeRepository().getNodes(networkType)));

    }

    public NetworkConfig(String baseDirPath,
                         NodeId nodeId,
                         NetworkType networkType,
                         PeerConfig peerConfig) {
        this.baseDirPath = baseDirPath;
        this.nodeId = nodeId;
        this.networkType = networkType;
        this.peerConfig = peerConfig;
    }
}
