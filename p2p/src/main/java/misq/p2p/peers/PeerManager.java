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

import lombok.extern.slf4j.Slf4j;
import misq.p2p.guard.Guard;
import misq.p2p.node.Address;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class PeerManager {

    private final Guard guard;
    private final Set<Address> seedNodes;
    private final PeerExchange peerExchange;

    public PeerManager(Guard guard, PeerGroup peerGroup, Set<Address> seedNodes) {
        this.guard = guard;
        this.seedNodes = seedNodes;

        peerExchange = new PeerExchange(guard, seedNodes);
    }

    public CompletableFuture<Boolean> bootstrap() {
        return peerExchange.bootstrap();
    }

    public void shutdown() {

    }
}
