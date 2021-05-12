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

package misq.p2p.confidential;

import misq.common.util.CollectionUtil;
import misq.p2p.NetworkType;
import misq.p2p.endpoint.Address;
import misq.p2p.endpoint.Connection;
import misq.p2p.endpoint.Message;
import misq.p2p.endpoint.MessageListener;
import misq.p2p.peers.PeerGroup;
import misq.p2p.protection.ProtectedNode;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

public class ConfidentialMessageService implements MessageListener {
    private final ProtectedNode protectedNode;
    private final PeerGroup peerGroup;
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();

    public ConfidentialMessageService(ProtectedNode protectedNode, PeerGroup peerGroup) {
        this.protectedNode = protectedNode;
        this.peerGroup = peerGroup;

        protectedNode.addMessageListener(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Connection connection, Message message) {
        if (message instanceof ConfidentialMessage) {
            ConfidentialMessage confidentialMessage = (ConfidentialMessage) message;
            if (confidentialMessage instanceof RelayMessage) {
                RelayMessage relayMessage = (RelayMessage) message;
                Address targetAddress = relayMessage.getTargetAddress();
                send(message, targetAddress);
            } else {
                messageListeners.forEach(listener -> {
                    Message unSealedMessage = unseal(confidentialMessage);
                    listener.onMessage(connection, unSealedMessage);
                });
            }
        }
    }

    public CompletableFuture<Connection> send(Message message, Address peerAddress) {
        return protectedNode.send(seal(message), peerAddress);
    }

    public CompletableFuture<Connection> relay(Message message, Address peerAddress) {
        Optional<Address> myAddress = protectedNode.getMyAddress();
        checkArgument(myAddress.isPresent());
        Set<Connection> connections = getConnectionsWithSupportedNetwork(peerAddress.getNetworkType());
        Connection outboundConnection = CollectionUtil.getRandomElement(connections);
        if (outboundConnection != null) {
            //todo we need 2 diff. pub keys for encryption here
            RelayMessage relayMessage = new RelayMessage(seal(message), peerAddress);
            return protectedNode.send(seal(relayMessage), outboundConnection);
        }
        return CompletableFuture.failedFuture(new Exception("No connection supporting that network type found."));
    }

    public void shutdown() {
        protectedNode.removeMessageListener(this);
        messageListeners.clear();
    }

    public CompletableFuture<Connection> send(Message message, Connection connection) {
        return protectedNode.send(seal(message), connection);
    }

    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        messageListeners.remove(messageListener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private Message unseal(ConfidentialMessage confidentialMessage) {
        return confidentialMessage.getMessage();
    }

    private ConfidentialMessage seal(Message message) {
        return new ConfidentialMessage(message);
    }

    private Set<Connection> getConnectionsWithSupportedNetwork(NetworkType networkType) {
        return peerGroup.getConnectedPeerByAddress().stream()
                .filter(peer -> peer.getCapability().getSupportedNetworkTypes().contains(networkType))
                .flatMap(peer -> protectedNode.findConnection(peer.getAddress()).stream())
                .collect(Collectors.toSet());
    }
}
