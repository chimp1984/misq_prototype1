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


import misq.common.util.MapUtils;
import misq.p2p.Address;
import misq.p2p.Message;
import misq.p2p.NetworkConfig;
import misq.p2p.NetworkType;
import misq.p2p.node.capability.Capability;
import misq.p2p.node.capability.CapabilityRequestHandler;
import misq.p2p.node.capability.CapabilityResponseHandler;
import misq.p2p.node.connection.InboundConnection;
import misq.p2p.node.connection.OutboundConnection;
import misq.p2p.node.connection.RawConnection;
import misq.p2p.node.proxy.GetServerSocketResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Responsibility:
 * - Creates the RawNode
 * - Request {@link Capability} from peer and transmit own {@link Capability}
 * <p>
 * Only after that initial handshake is completed messages can be sent and received.
 * When attempting to send a Message while the handshake is not completed the message will kept in a queue for
 * being processed once the handshake is completed.
 * <p>
 * ConnectionListeners on that node will only be notified about new connections after the handshake is completed.
 * MessageListeners on that node will only be notified about new messages after the handshake is completed.
 */
public class BaseNode implements RawNode.ConnectionListener, MessageListener, RawConnection.MessageListener {
    private static final Logger log = LoggerFactory.getLogger(BaseNode.class);

    private final RawNode rawNode;
    private final MessageListener messageHandler;
    private final Set<NetworkType> mySupportedNetworks;

    // ConnectionUid is key in following maps
    private final Map<String, CapabilityResponseHandler> responseHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, CapabilityRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    private final Map<String, RawConnection.MessageListener> messageListenerMap = new ConcurrentHashMap<>();

    private final Set<Address> addressesOfPendingConnections = new CopyOnWriteArraySet<>();
    private final Map<Address, List<CompletableFuture<Connection>>> pendingConnectionQueue = new ConcurrentHashMap<>();

    private final Set<ConnectionListener> connectionListeners = new CopyOnWriteArraySet<>();

    private final Object isStoppedLock = new Object();
    private volatile boolean isStopped;

    BaseNode(NetworkConfig networkConfig, Set<NetworkType> mySupportedNetworks, MessageListener messageHandler) {
        this.mySupportedNetworks = mySupportedNetworks;
        this.messageHandler = messageHandler;

        rawNode = new RawNode(networkConfig);
        rawNode.addConnectionListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // RawNode.ConnectionListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onInboundConnection(InboundConnection inboundConnection) {
        setupResponseHandler(inboundConnection);
    }

    @Override
    public void onOutboundConnection(OutboundConnection outboundConnection, Address peerAddress) {
        setupResponseHandler(outboundConnection);
    }

    @Override
    public void onDisconnect(RawConnection rawConnection) {
        String id = rawConnection.getId();

        MapUtils.disposeAndRemove(id, requestHandlerMap);
        MapUtils.disposeAndRemove(id, requestHandlerMap);

        rawNode.findPeerAddress(rawConnection).ifPresent(address -> {
            if (pendingConnectionQueue.containsKey(address)) {
                pendingConnectionQueue.get(address).forEach(future ->
                        future.completeExceptionally(new Exception("Connection has been closed while the message was pending to be sent.")));
                pendingConnectionQueue.remove(address);
                addressesOfPendingConnections.remove(address);
            }
        });

        if (connectionMap.containsKey(id)) {
            // We only notify higher layers on disconnects if established connections, as they should not concern about
            // connections which have not reached that state.
            Connection connection = connectionMap.get(id);
            connectionMap.remove(id);
            connectionListeners.forEach(listener -> listener.onDisconnect(connection));

            if (messageListenerMap.containsKey(id)) {
                RawConnection.MessageListener messageListener = messageListenerMap.get(id);
                connection.getRawConnection().removeMessageListener(messageListener);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Connection.MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message, Connection connection) {
        messageHandler.onMessage(message, connection);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // RawConnection.MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message) {

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    CompletableFuture<Connection> getConnection(Address peerAddress) {
        return findConnection(peerAddress)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> {
                    // In case we have a pending connection request for that address we queue it for later processing
                    if (addressesOfPendingConnections.contains(peerAddress)) {
                        return getFutureOfPendingConnection(peerAddress);
                    } else {
                        addressesOfPendingConnections.add(peerAddress);
                        return rawNode.getOrCreateConnection(peerAddress)
                                .thenCompose(connection -> requestCapability(connection, peerAddress))
                                .whenComplete((connection, throwable) ->
                                        completeFuturesOfPendingConnection(peerAddress, connection));
                    }
                });
    }

    CompletableFuture<Connection> send(Message message, Address peerAddress) {
        return getConnection(peerAddress)
                .thenCompose(connection -> send(message, connection));
    }

    CompletableFuture<Connection> send(Message message, Connection connection) {
        return rawNode.send(message, connection.getRawConnection())
                .thenApply(rawConnection -> connection);
    }

    void shutdown() {
        synchronized (isStoppedLock) {
            isStopped = true;
        }
        connectionListeners.clear();

        rawNode.removeConnectionListener(this);

        pendingConnectionQueue.values().stream()
                .flatMap(Collection::stream)
                .forEach(future -> future.cancel(true));
        pendingConnectionQueue.clear();

        addressesOfPendingConnections.clear();

        MapUtils.disposeAndRemoveAll(requestHandlerMap);
        MapUtils.disposeAndRemoveAll(responseHandlerMap);
        connectionMap.clear();

        rawNode.shutdown();
    }

    Optional<Connection> findConnection(Address peerAddress) {
        return connectionMap.values().stream()
                .filter(c -> c.getPeerAddress().equals(peerAddress))
                .findAny();
    }

    void addConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.add(connectionListener);
    }

    void removeConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.remove(connectionListener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Delegates
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    CompletableFuture<GetServerSocketResult> initializeServer(String serverId, int serverPort) {
        return rawNode.initializeServer(serverId, serverPort);
    }

    Optional<Address> findMyAddress() {
        return rawNode.findMyAddress();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<Connection> requestCapability(RawConnection rawConnection, Address peerAddress) {
        CapabilityRequestHandler capabilityRequestHandler = new CapabilityRequestHandler(rawConnection,
                peerAddress, getMyAddress(), mySupportedNetworks);
        requestHandlerMap.put(rawConnection.getId(), capabilityRequestHandler);
        return capabilityRequestHandler.request()
                .thenCompose(capability -> {
                    log.info("onOutboundConnection: peerAddress: {}, myAddress={}, rawConnection: {}", peerAddress, getMyAddress(), rawConnection);
                    requestHandlerMap.remove(rawConnection.getId());
                    Connection connection = new Connection(rawConnection, capability);
                    onConnection(connection);
                    return CompletableFuture.completedFuture(connection);
                });
    }

    private void setupResponseHandler(RawConnection rawConnection) {
        if (isStopped) {
            return;
        }
        Address myAddress = getMyAddress();
        String id = rawConnection.getId();
        CapabilityResponseHandler capabilityResponseHandler = new CapabilityResponseHandler(rawConnection,
                myAddress,
                mySupportedNetworks,
                capability -> {
                    if (!isStopped) {
                        log.info("setupResponseHandler: peerAddress: {}, myAddress={}, rawConnection: {}",
                                capability.getAddress(), myAddress, rawConnection);
                        responseHandlerMap.remove(id);
                        Connection connection = new Connection(rawConnection, capability);
                        onConnection(connection);
                    }
                });
        responseHandlerMap.put(id, capabilityResponseHandler);
    }

    private void onConnection(Connection connection) {
        String connectionId = connection.getId();
        RawConnection.MessageListener messageListener = message -> BaseNode.this.onMessage(message, connection);
        messageListenerMap.put(connection.getId(), messageListener);
        connection.getRawConnection().addMessageListener(messageListener);
        connectionMap.put(connectionId, connection);
        connectionListeners.forEach(listener -> listener.onConnection(connection));
    }

    private CompletableFuture<Connection> getFutureOfPendingConnection(Address peerAddress) {
        pendingConnectionQueue.putIfAbsent(peerAddress, new ArrayList<>());
        List<CompletableFuture<Connection>> list = pendingConnectionQueue.get(peerAddress);
        CompletableFuture<Connection> future = new CompletableFuture<>();
        list.add(future);
        return future;
    }

    private void completeFuturesOfPendingConnection(Address peerAddress, Connection connection) {
        if (connection != null) {
            if (pendingConnectionQueue.containsKey(peerAddress)) {
                pendingConnectionQueue.get(peerAddress)
                        .forEach(future -> future.complete(connection));
            }
            pendingConnectionQueue.remove(peerAddress);
            addressesOfPendingConnections.remove(peerAddress);
        }
    }

    private Address getMyAddress() {
        Optional<Address> myAddress = findMyAddress();
        checkArgument(myAddress.isPresent(), "getMyAddress must not be called before node is set up.");
        return myAddress.get();
    }
}
