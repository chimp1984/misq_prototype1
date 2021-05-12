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

package misq.p2p.peers.exchange;

import lombok.Getter;

@Getter
public class PeerExchangeConfig {
    private final int numSeeNodesAtBoostrap;
    private final int numPersistedPeersAtBoostrap;
    private final int numReportedPeersAtBoostrap;
    private final int repeatPeerExchangeDelay;

    public PeerExchangeConfig() {
        this(2, 8, 4, 300);
    }

    public PeerExchangeConfig(int numSeeNodesAtBoostrap,
                              int numPersistedPeersAtBoostrap,
                              int numReportedPeersAtBoostrap,
                              int repeatPeerExchangeDelay) {
        this.numSeeNodesAtBoostrap = numSeeNodesAtBoostrap;
        this.numPersistedPeersAtBoostrap = numPersistedPeersAtBoostrap;
        this.numReportedPeersAtBoostrap = numReportedPeersAtBoostrap;
        this.repeatPeerExchangeDelay = repeatPeerExchangeDelay;
    }
}
