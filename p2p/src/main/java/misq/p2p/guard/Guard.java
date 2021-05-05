/*
 * This file is part of Misq.
 *
 * Misq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Misq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Misq. If not, see <http://www.gnu.org/licenses/>.
 */

package misq.p2p.guard;


import misq.p2p.NetworkType;
import misq.p2p.capability.CapabilityExchange;
import misq.p2p.node.*;
import misq.p2p.proxy.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Responsibility:
 * - Adds an AccessToken to the outgoing message.
 * - On received messages checks with the permissionControl if the AccessToken is valid.
 */
public class Guard implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(Guard.class);

    private final PermissionControl permissionControl;
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();
    private final CapabilityExchange capabilityExchange;

    public Guard(CapabilityExchange capabilityExchange) {
        this.capabilityExchange = capabilityExchange;

        this.permissionControl = new NoRestriction();

        capabilityExchange.addMessageListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Connection connection, Message message) {
        if (message instanceof GuardedMessage) {
            GuardedMessage guardedMessage = (GuardedMessage) message;
            if (permissionControl.hasPermit(guardedMessage)) {
                log.info("Received valid restrictedMessage: {}", guardedMessage);
                messageListeners.forEach(listener -> listener.onMessage(connection, guardedMessage.getMessage()));
            } else {
                log.warn("Handling message at onMessage is not permitted by guard");
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Connection> send(Message message, Address peerAddress) {
        return capabilityExchange.getConnection(peerAddress)
                .thenCompose(connection -> send(message, connection));
    }

    public CompletableFuture<Connection> send(Message message, Connection connection) {
        return permissionControl.getPermit(message)
                .thenCompose(permit ->
                        capabilityExchange.send(new GuardedMessage(message, permit), connection));
    }

    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        messageListeners.remove(messageListener);
    }

    public void shutdown() {
        messageListeners.clear();
        capabilityExchange.removeMessageListener(this);
        permissionControl.shutdown();
        capabilityExchange.shutdown();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Delegates
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<ServerInfo> bootstrap() {
        return capabilityExchange.bootstrap();
    }

    public void addConnectionListener(ConnectionListener listener) {
        capabilityExchange.addConnectionListener(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        capabilityExchange.removeConnectionListener(listener);
    }

    public Optional<Address> getMyAddress() {
        return capabilityExchange.getMyAddress();
    }

    public Set<OutboundConnection> getConnectionsWithSupportedNetwork(NetworkType networkType) {
        return capabilityExchange.getConnectionsWithSupportedNetwork(networkType);
    }

    public CompletableFuture<Connection> getConnection(Address peerAddress) {
        return capabilityExchange.getConnection(peerAddress);
    }

    public Optional<Address> getPeerAddress(Connection connection) {
        return capabilityExchange.getPeerAddress(connection);
    }
}
