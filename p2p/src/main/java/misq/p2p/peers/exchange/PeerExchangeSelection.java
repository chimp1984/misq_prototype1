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
import misq.p2p.peers.PeerConfig;
import misq.p2p.peers.PeerGroup;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements the strategy how to select the peers used in peer management and exchange
 */
public class PeerExchangeSelection {
    private final PeerGroup peerGroup;
    private final PeerConfig peerConfig;

    public PeerExchangeSelection(PeerGroup peerGroup, PeerConfig peerConfig) {
        this.peerGroup = peerGroup;
        this.peerConfig = peerConfig;
    }

    public void addPeersFromPeerExchange(Set<Peer> peers) {
        peerGroup.addReportedPeers(peers);
    }

    public Set<Peer> getPeersForPeerExchange() {
        List<Peer> list = peerGroup.getReportedPeers().stream()
                .sorted(Comparator.comparing(Peer::getDate))
                .limit(100)
                .collect(Collectors.toList());
        list.addAll(peerGroup.getAllConnectedPeers());
        return new HashSet<>(list);
    }

    public Set<Address> getAddressesForBoostrap() {
        Stream<Address> seeds = peerGroup.getSeedNodes().stream()
                .limit(peerConfig.getNumSeeNodesAtBoostrap());
        Stream<Address> reported = peerGroup.getReportedPeers().stream()
                .map(Peer::getAddress)
                .limit(peerConfig.getNumPersistedPeersAtBoostrap());

        Set<Address> collected = Stream.concat(seeds, reported)
                .collect(Collectors.toSet());

        int missing = peerConfig.getMinNumConnectedPeers() - collected.size();
        if (missing > 0) {
            if (!peerGroup.getReportedPeers().isEmpty()) {
                Set<Address> livePeers = peerGroup.getReportedPeers().stream()
                        .map(Peer::getAddress)
                        .limit(missing)
                        .collect(Collectors.toSet());
                collected.addAll(livePeers);
            }
        }
        return collected;

    }

    public boolean sufficientPeersAtPeerExchange() {
        return sufficientConnections() && sufficientReportedPeers();
    }

    private boolean sufficientReportedPeers() {
        return peerGroup.getReportedPeers().size() >= peerConfig.getMinNumReportedPeers();
    }

    private boolean sufficientConnections() {
        return peerGroup.getAllConnectedPeers().size() >= peerConfig.getMinNumConnectedPeers();
    }
}
