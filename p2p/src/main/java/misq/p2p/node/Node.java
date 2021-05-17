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

package misq.p2p.node;


import misq.p2p.Address;
import misq.p2p.NetworkConfig;
import misq.p2p.NetworkType;
import misq.p2p.message.Message;
import misq.p2p.node.protection.GuardedMessage;
import misq.p2p.node.protection.NoRestriction;
import misq.p2p.node.protection.PermissionControl;
import misq.p2p.node.proxy.GetServerSocketResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Responsibility:
 * - Creates BaseNode
 * - Adds an AccessToken to the outgoing message.
 * - On received messages checks with the permissionControl if the AccessToken is valid.
 * <p>
 * TODO make PermissionControl mocks for BSQ bonded or LN (sphinx) based transport layer to see if other monetary token based ddos
 * protection strategies work inside the current design
 */
public class Node implements MessageListener, ConnectionListener {
    private static final Logger log = LoggerFactory.getLogger(Node.class);

    private final PermissionControl permissionControl;
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();
    private final Set<ConnectionListener> connectionListeners = new CopyOnWriteArraySet<>();
    private final BaseNode baseNode;
    private final Object isStoppedLock = new Object();
    private volatile boolean isStopped;

    public Node(NetworkConfig networkConfig, Set<NetworkType> mySupportedNetworks) {
        baseNode = new BaseNode(networkConfig, mySupportedNetworks, this);
        permissionControl = new NoRestriction();

        baseNode.addConnectionListener(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ConnectionListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onConnection(Connection connection) {
        connectionListeners.forEach(listener -> listener.onConnection(connection));
    }


    @Override
    public void onDisconnect(Connection connection) {
        connectionListeners.forEach(listener -> listener.onDisconnect(connection));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message, Connection connection) {
        if (message instanceof GuardedMessage && !isStopped) {
            GuardedMessage guardedMessage = (GuardedMessage) message;
            if (permissionControl.hasPermit(guardedMessage)) {
                Message payload = guardedMessage.getPayload();
                // connection.notifyListeners(payload);
                messageListeners.forEach(listener -> listener.onMessage(payload, connection));
            } else {
                log.warn("Handling message at onMessage is not permitted by guard");
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Connection> send(Message message, Address peerAddress) {
        return baseNode.getConnection(peerAddress)
                .thenCompose(connection -> send(message, connection));
    }

    public CompletableFuture<Connection> send(Message message, Connection connection) {
        return permissionControl.getPermit(message)
                .thenCompose(permit -> baseNode.send(new GuardedMessage(message, permit), connection));
    }

    public void shutdown() {
        synchronized (isStoppedLock) {
            isStopped = true;
        }
        messageListeners.clear();
        connectionListeners.clear();
        baseNode.removeConnectionListener(this);
        permissionControl.shutdown();
        baseNode.shutdown();
    }

    // Only to be used when we know that we have already created the default server
    public Address getMyAddress() {
        Optional<Address> myAddress = baseNode.findMyAddress();
        checkArgument(myAddress.isPresent(), "myAddress need to be present at a getMyAddress call");
        return myAddress.get();
    }

    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        messageListeners.remove(messageListener);
    }

    public void addConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.add(connectionListener);
    }

    public void removeConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.remove(connectionListener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Delegates
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<GetServerSocketResult> initializeServer(String serverId, int serverPort) {
        return baseNode.initializeServer(serverId, serverPort);
    }

    public Optional<Address> findMyAddress() {
        return baseNode.findMyAddress();
    }


    public CompletableFuture<Connection> getConnection(Address peerAddress) {
        return baseNode.getConnection(peerAddress);
    }

    public Optional<Connection> findConnection(Address peerAddress) {
        return baseNode.findConnection(peerAddress);
    }
}
