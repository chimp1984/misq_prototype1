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

package misq.p2p.protection;


import misq.p2p.NetworkType;
import misq.p2p.capability.Capability;
import misq.p2p.capability.CapabilityAwareNode;
import misq.p2p.endpoint.*;
import misq.p2p.proxy.GetServerSocketResult;
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
 * <p>
 * TODO make PermissionControl mocks for BSQ bonded or LN (sphinx) based transport layer to see if other monetary token based ddos
 * protection strategies work inside the current design
 */
public class ProtectedNode implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(ProtectedNode.class);

    private final PermissionControl permissionControl;
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();
    private final CapabilityAwareNode capabilityAwareNode;
    private final Object isStoppedLock = new Object();
    private volatile boolean isStopped;

    public ProtectedNode(CapabilityAwareNode capabilityAwareNode) {
        this.capabilityAwareNode = capabilityAwareNode;

        this.permissionControl = new NoRestriction();

        capabilityAwareNode.addMessageListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Connection connection, Message message) {
        if (message instanceof GuardedMessage && !isStopped) {
            GuardedMessage guardedMessage = (GuardedMessage) message;
            if (permissionControl.hasPermit(guardedMessage)) {
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
        return capabilityAwareNode.getConnection(peerAddress)
                .thenCompose(connection -> send(message, connection));
    }

    public CompletableFuture<Connection> send(Message message, Connection connection) {
        return permissionControl.getPermit(message)
                .thenCompose(permit -> capabilityAwareNode.send(new GuardedMessage(message, permit), connection));
    }

    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        messageListeners.remove(messageListener);
    }

    public void shutdown() {
        synchronized (isStoppedLock) {
            isStopped = true;
        }
        messageListeners.clear();
        capabilityAwareNode.removeMessageListener(this);
        permissionControl.shutdown();
        capabilityAwareNode.shutdown();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Delegates
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<GetServerSocketResult> initializeServer(String serverId, int serverPort) {
        return capabilityAwareNode.initializeServer(serverId, serverPort);
    }

    public void addConnectionListener(ConnectionListener listener) {
        capabilityAwareNode.addConnectionListener(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        capabilityAwareNode.removeConnectionListener(listener);
    }

    public Optional<Address> getMyAddress() {
        return capabilityAwareNode.getMyAddress();
    }

    public Set<OutboundConnection> getConnectionsWithSupportedNetwork(NetworkType networkType) {
        return capabilityAwareNode.getConnectionsWithSupportedNetwork(networkType);
    }

    public CompletableFuture<Connection> getConnection(Address peerAddress) {
        return capabilityAwareNode.getConnection(peerAddress);
    }

    public Optional<Connection> findConnection(Address peerAddress) {
        return capabilityAwareNode.findConnection(peerAddress);
    }

    public Optional<Address> getPeerAddress(Connection connection) {
        return capabilityAwareNode.getPeerAddress(connection);
    }

    public Capability getCapability(Connection connection) {
        return capabilityAwareNode.getCapability(connection);
    }

}
