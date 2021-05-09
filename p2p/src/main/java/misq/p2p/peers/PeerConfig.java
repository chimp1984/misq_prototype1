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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import misq.p2p.node.Address;

import java.util.List;

@Getter
@EqualsAndHashCode
public class PeerConfig {
    private final List<Address> seedNodes;
    private final int numPersistedPeersAtBoostrap;
    private final int numSeeNodesAtBoostrap;
    private final int minNumConnectedPeers;
    private final int maxNumConnectedPeers;
    private final int minNumReportedPeers;

    public PeerConfig(List<Address> seedNodes) {
        this.seedNodes = seedNodes;

        numPersistedPeersAtBoostrap = 10;
        numSeeNodesAtBoostrap = 2;
        minNumConnectedPeers = 8;
        maxNumConnectedPeers = 12;
        minNumReportedPeers = 50;
    }
}
