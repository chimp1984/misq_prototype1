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

import misq.p2p.guard.Guard;
import misq.p2p.node.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PeerGroup implements ConnectionListener {
    private final Guard guard;
    private final Map<Address, Peer> connectedPeers = new ConcurrentHashMap<>();
    private final Map<Address, Connection> connections = new ConcurrentHashMap<>();

    public PeerGroup(Guard guard) {
        this.guard = guard;
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
        guard.getPeerAddress(connection).ifPresent(connectedPeers::remove);
    }

    public Set<Address> getConnectedPeerAddresses() {
        return connectedPeers.keySet();
    }

    public void shutdown() {

    }

    private void addPeer(Connection connection) {
        guard.getPeerAddress(connection).ifPresent(this::addPeer);
    }

    private void addPeer(Address peerAddress) {
        connectedPeers.put(peerAddress, new Peer(peerAddress));
    }


}
