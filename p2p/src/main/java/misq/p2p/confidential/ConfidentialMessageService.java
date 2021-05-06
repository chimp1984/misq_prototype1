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
import misq.p2p.guard.Guard;
import misq.p2p.node.*;
import net.i2p.util.ConcurrentHashSet;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkArgument;

public class ConfidentialMessageService implements MessageListener, ConnectionListener {
    private final Guard guard;
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();
    private final Set<Connection> connections = new ConcurrentHashSet<>();

    public ConfidentialMessageService(Guard guard) {
        this.guard = guard;

        guard.addMessageListener(this);
        guard.addConnectionListener(this);
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

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ConnectionListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public void onInboundConnection(InboundConnection connection) {
    }

    @Override
    public void onOutboundConnection(OutboundConnection connection, Address peerAddress) {
        connections.add(connection);
    }

    @Override
    public void onDisconnect(Connection connection) {
        connections.remove(connection);
    }

    public CompletableFuture<Connection> send(Message message, Address peerAddress) {
        return guard.send(seal(message), peerAddress);
    }

    public CompletableFuture<Connection> relay(Message message, Address peerAddress) {
        Optional<Address> myAddress = guard.getMyAddress();
        checkArgument(myAddress.isPresent());
        Set<OutboundConnection> connections = guard.getConnectionsWithSupportedNetwork(peerAddress.getNetworkType());
        OutboundConnection outboundConnection = CollectionUtil.getRandomElement(connections);
        if (outboundConnection != null) {
            //todo we need 2 diff. pubkeys for encryption here
            RelayMessage relayMessage = new RelayMessage(seal(message), peerAddress);
            return guard.send(seal(relayMessage), outboundConnection);
        }
        return CompletableFuture.failedFuture(new Exception("No connection supporting that network type found."));
    }


    public CompletableFuture<Connection> send(Message message, Connection connection) {
        return guard.send(seal(message), connection);
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

    public void shutdown() {
        guard.removeMessageListener(this);
        messageListeners.clear();
    }

}
