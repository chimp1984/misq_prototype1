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

import misq.p2p.node.Address;
import misq.p2p.peers.Peer;

import java.util.Set;

/**
 * Strategy how to select the peers used in peer management and exchange.
 */
public interface PeerExchangeStrategy {
    void addPeersFromPeerExchange(Set<Peer> peers);

    Set<Peer> getPeersForPeerExchange(Address peerAddress);

    Set<Address> getAddressesForBootstrap();

    boolean repeatBootstrap(long numSuccess, int numFutures);

    long getRepeatBootstrapDelay();
}
