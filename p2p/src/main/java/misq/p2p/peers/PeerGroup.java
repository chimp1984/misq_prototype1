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
import lombok.extern.slf4j.Slf4j;
import misq.p2p.Address;
import misq.p2p.node.Connection;
import misq.p2p.node.ConnectionListener;
import misq.p2p.node.Node;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Maintains different collections of peers and connections
 */
@Slf4j
public class PeerGroup implements ConnectionListener {
    private final Node node;
    @Getter
    private final ImmutableList<Address> seedNodes;
    private final Map<Address, Peer> connectedPeerByAddress = new ConcurrentHashMap<>();
    public final int serverPort;


    public Set<Peer> getReportedPeers() {
        return reportedPeers;
    }

    // @Getter
    private final Set<Peer> reportedPeers = new CopyOnWriteArraySet<>();
    @Getter
    private final Set<Peer> persistedPeers = new CopyOnWriteArraySet<>();
    @Getter
    private final Set<Connection> connections = new CopyOnWriteArraySet<>();

    public PeerGroup(Node node, PeerConfig peerConfig, int serverPort) {
        this.serverPort = serverPort;
        this.node = node;

        List<Address> seeds = new ArrayList<>(peerConfig.getSeedNodes());
        Collections.shuffle(seeds);
        seedNodes = ImmutableList.copyOf(seeds);

        node.addConnectionListener(this);
    }

    @Override
    public void onConnection(Connection connection) {
        Peer peer = new Peer(connection.getCapability());
        connectedPeerByAddress.put(peer.getAddress(), peer);
        connections.add(connection);
    }

    @Override
    public void onDisconnect(Connection connection) {
        connectedPeerByAddress.remove(connection.getPeerAddress());
    }

    public Set<Address> getConnectedPeerAddresses() {
        return connectedPeerByAddress.keySet();
    }

    public Collection<Peer> getConnectedPeerByAddress() {
        return connectedPeerByAddress.values();
    }

    public void addReportedPeers(Set<Peer> peers) {
        reportedPeers.addAll(peers);
    }

    public Set<Peer> getAllConnectedPeers() {
        return new HashSet<>(connectedPeerByAddress.values());
    }

    public boolean notMyself(Address address) {
        Optional<Address> optionalMyAddress = node.findMyAddress();
        return !optionalMyAddress.isPresent() || !optionalMyAddress.get().equals(address);
    }

    public boolean notMyself(Peer peer) {
        return notMyself(peer.getAddress());
    }

    public boolean notASeed(Address address) {
        return seedNodes.stream().noneMatch(e -> e.equals(address));
    }

    public boolean notASeed(Peer peer) {
        return notASeed(peer.getAddress());
    }
}
