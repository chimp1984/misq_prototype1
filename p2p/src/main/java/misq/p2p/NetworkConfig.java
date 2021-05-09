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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import misq.p2p.peers.PeerConfig;

@Getter
@EqualsAndHashCode
public class NetworkConfig {
    private final String baseDirName;
    private final NetworkType networkType;
    private final String serverId;
    private final int serverPort;
    private final PeerConfig peerConfig;

    public NetworkConfig(String baseDirName,
                         NetworkType networkType,
                         String serverId,
                         int serverPort) {
        this(baseDirName,
                networkType,
                serverId,
                serverPort,
                new PeerConfig(new SeedNodeRepository().getNodes(networkType)));
    }

    public NetworkConfig(String baseDirName,
                         NetworkType networkType,
                         String serverId,
                         int serverPort,
                         PeerConfig peerConfig) {
        this.baseDirName = baseDirName;
        this.networkType = networkType;
        this.serverId = serverId;
        this.serverPort = serverPort;
        this.peerConfig = peerConfig;
    }
}
