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

import lombok.extern.slf4j.Slf4j;
import misq.p2p.endpoint.Address;
import misq.p2p.peers.Peer;
import misq.p2p.peers.PeerConfig;
import misq.p2p.peers.PeerGroup;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple implements the strategy how to select the peers for peer exchange.
 */
@Slf4j
public class DefaultPeerExchangeStrategy implements PeerExchangeStrategy {
    private final PeerGroup peerGroup;
    private final PeerConfig peerConfig;
    private final PeerExchangeConfig peerExchangeConfig;
    // We keep track of the addresses we contacted so in case we need to make a repeated request round that we do not \
    // pick the same addresses.
    private final Set<Address> usedAddresses = new HashSet<>();

    public DefaultPeerExchangeStrategy(PeerGroup peerGroup, PeerConfig peerConfig) {
        this.peerGroup = peerGroup;
        this.peerConfig = peerConfig;
        this.peerExchangeConfig = peerConfig.getPeerExchangeConfig();
    }

    @Override
    public void addPeersFromPeerExchange(Set<Peer> peers, Address senderAddress) {
        Set<Peer> collect = peers.stream()
                .filter(peerGroup::notMyself)
                .collect(Collectors.toSet());
        peerGroup.addReportedPeers(collect);
    }

    @Override
    public Set<Peer> getPeersForPeerExchange(Address peerAddress) {
        List<Peer> list = peerGroup.getReportedPeers().stream()
                .sorted(Comparator.comparing(Peer::getDate))
                .limit(100)
                .collect(Collectors.toList());
        Set<Peer> allConnectedPeers = peerGroup.getAllConnectedPeers();
        list.addAll(allConnectedPeers);
        return list.stream()
                .filter(peerGroup::notASeed)
                .filter(peer -> notDirectPeer(peerAddress, peer))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Address> getAddressesForBootstrap() {
        int numSeeNodesAtBoostrap = peerExchangeConfig.getNumSeeNodesAtBoostrap();
        int numPersistedPeersAtBoostrap = peerExchangeConfig.getNumPersistedPeersAtBoostrap();
        int numReportedPeersAtBoostrap = peerExchangeConfig.getNumReportedPeersAtBoostrap();
        int minNumConnectedPeers = peerConfig.getMinNumConnectedPeers();

        Set<Address> seeds = peerGroup.getSeedNodes().stream()
                .filter(peerGroup::notMyself)
                .filter(this::notUsedYet)
                .limit(numSeeNodesAtBoostrap)
                .collect(Collectors.toSet()); //2

        // Usually we don't have reported peers at startup, but in case or repeated bootstrap attempts we likely have
        // as well it could be that other nodes have started peer exchange to ourself before we start the peer exchange.
        Set<Address> reported = peerGroup.getReportedPeers().stream()
                .map(Peer::getAddress)
                .filter(this::notUsedYet)
                .limit(numReportedPeersAtBoostrap)
                .collect(Collectors.toSet()); //4

        Set<Address> persisted = peerGroup.getPersistedPeers().stream()
                .map(Peer::getAddress)
                .filter(this::notUsedYet)
                .limit(numPersistedPeersAtBoostrap)
                .collect(Collectors.toSet()); //8

        Set<Address> connectedPeerAddresses = peerGroup.getConnectedPeerAddresses().stream()
                .filter(this::notUsedYet)
                .filter(peerGroup::notASeed)
                .collect(Collectors.toSet());

        // If we have already connections (at repeated bootstraps) we limit the new set to what is missing to reach out
        // target.
        int numConnections = peerGroup.getAllConnectedPeers().size();
        int candidates = seeds.size() + reported.size() + persisted.size();
        int missingConnections = numConnections > 0 ?
                minNumConnectedPeers - numConnections //8
                : candidates;
        missingConnections = Math.max(0, missingConnections);

        // In case we apply the limit it will be applied at the persisted first which is intended as those are the least
        // likely to be successful.
        List<Address> all = new ArrayList<>(seeds);
        all.addAll(reported);
        all.addAll(persisted);
        all.addAll(connectedPeerAddresses);

        Set<Address> result = all.stream()
                .limit(missingConnections)
                .collect(Collectors.toSet());
        usedAddresses.addAll(result);
        return result;
    }

    @Override
    public boolean repeatBootstrap(long numSuccess, int numFutures) {
        return numSuccess == 0 ||
                numFutures - numSuccess > 4 ||
                !sufficientConnections() ||
                !sufficientReportedPeers();
    }

    @Override
    public long getRepeatBootstrapDelay() {
        return peerExchangeConfig.getRepeatPeerExchangeDelay();
    }

    private boolean sufficientReportedPeers() {
        return peerGroup.getReportedPeers().size() >= peerConfig.getMinNumReportedPeers();
    }

    private boolean sufficientConnections() {
        return peerGroup.getAllConnectedPeers().size() >= peerConfig.getMinNumConnectedPeers();
    }

    private boolean notUsedYet(Address address) {
        return !usedAddresses.contains(address);
    }

    private boolean notDirectPeer(Address peerAddress, Peer peer) {
        return !peer.getAddress().equals(peerAddress);
    }
}
