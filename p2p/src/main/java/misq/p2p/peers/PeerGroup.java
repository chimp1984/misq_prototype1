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

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import misq.common.util.CollectionUtil;
import misq.p2p.guard.Guard;
import misq.p2p.node.*;
import net.i2p.util.ConcurrentHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maintains different collections of peers and connections
 */
public class PeerGroup implements ConnectionListener {
    private final Guard guard;
    @Getter
    private final ImmutableList<Address> seedNodes;
    private final Map<Address, Peer> connectedPeerByAddress = new ConcurrentHashMap<>();
    @Getter
    private final Set<Peer> reportedPeers = new ConcurrentHashSet<>();
    @Getter
    private final Set<Peer> persistedPeers = new ConcurrentHashSet<>();
    private final Map<Address, Connection> connectionByAddress = new ConcurrentHashMap<>();

    public PeerGroup(Guard guard, PeerConfig peerConfig) {
        this.guard = guard;

        List<Address> seeds = new ArrayList<>(peerConfig.getSeedNodes());
        Collections.shuffle(seeds);
        seedNodes = ImmutableList.copyOf(seeds);

        guard.addConnectionListener(this);
    }

    @Override
    public void onInboundConnection(InboundConnection connection) {
        addPeer(connection);
        addConnection(connection);
    }

    private void addConnection(Connection connection) {
        //connections.put()
    }

    @Override
    public void onOutboundConnection(OutboundConnection connection, Address peerAddress) {
        addPeer(connection);
    }

    @Override
    public void onDisconnect(Connection connection) {
        guard.getPeerAddress(connection).ifPresent(connectedPeerByAddress::remove);
    }

    public Set<Address> getConnectedPeerAddresses() {
        return connectedPeerByAddress.keySet();
    }

    public Collection<Peer> getConnectedPeerByAddress() {
        return connectedPeerByAddress.values();
    }

    public Optional<Address> getRandomSeedNode() {
        if (seedNodes.isEmpty()) {
            return Optional.empty();
        }
        Address candidate = checkNotNull(CollectionUtil.getRandomElement(seedNodes));
        return Optional.of(candidate);
    }

    public void shutdown() {

    }

    private void addPeer(Connection connection) {
        Peer peer = new Peer(guard.getCapability(connection.getUid()));
        connectedPeerByAddress.put(peer.getAddress(), peer);
    }

    public void addReportedPeers(Set<Peer> peers) {
        reportedPeers.addAll(peers);
    }


    public Set<Peer> getAllConnectedPeers() {
        return new HashSet<>(connectedPeerByAddress.values());
    }

   /* public List<Address> getAllConnectedPeers(long limit) {
        return getRandomizedPeerAddresses(connectedPeerByAddress.values(), limit);
    }*/

    private List<Address> getAllRandomizedPeerAddresses(Collection<Peer> peers) {
        return getRandomizedPeerAddresses(peers, peers.size());
    }

    private List<Address> getRandomizedPeerAddresses(Collection<Peer> peers, int limit) {
        List<Address> list = peers.stream()
                .map(Peer::getAddress)
                .limit(limit)
                .collect(Collectors.toList());
        Collections.shuffle(list);
        return list;
    }
}
