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

package misq.p2p.capability;


import misq.common.util.MapUtils;
import misq.common.util.Tuple2;
import misq.p2p.NetworkType;
import misq.p2p.node.*;
import misq.p2p.proxy.GetServerSocketResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Responsibility:
 * - Request {@link Capability} from peer and transmit own {@link Capability}
 * <p>
 * Only after that initial handshake is completed messages can be sent and received.
 * When attempting to send a Message while the handshake is not completed the message will kept in a queue for
 * being processed once the handshake is completed.
 * <p>
 * ConnectionListeners on that node will only be notified about new connections after the handshake is completed.
 * Only onDisconnect will be called in any case on connectionListeners.
 * MessageListeners on that node will only be notified about new messages after the handshake is completed.
 */
public class CapabilityExchange implements ConnectionListener, MessageListener {
    private static final Logger log = LoggerFactory.getLogger(CapabilityExchange.class);

    private final Node node;
    private final Set<NetworkType> mySupportedNetworks;

    // ConnectionUid is key in following maps
    private final Map<String, CapabilityResponseHandler> responseHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, CapabilityRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, Capability> capabilityMap = new ConcurrentHashMap<>();

    private final Set<ConnectionListener> connectionListeners = new CopyOnWriteArraySet<>();
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();
    private final Map<String, List<Tuple2<Message, CompletableFuture<Connection>>>> sendQueue = new ConcurrentHashMap<>();
    private final Object isStoppedLock = new Object();
    private volatile boolean isStopped;

    public CapabilityExchange(Node node, Set<NetworkType> mySupportedNetworks) {
        this.node = node;
        this.mySupportedNetworks = mySupportedNetworks;

        node.addConnectionListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    // We register our handler only after handshake is completed
    @Override
    public void onMessage(Connection connection, Message message) {
        messageListeners.forEach(listener -> listener.onMessage(connection, message));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ConnectionListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onInboundConnection(InboundConnection connection) {
        if (isStopped) {
            return;
        }
        checkArgument(getMyAddress().isPresent(), "My address need to be already available.");
        CapabilityResponseHandler capabilityResponseHandler = new CapabilityResponseHandler(connection,
                getMyAddress().get(),
                mySupportedNetworks,
                capability -> {
                    if (!isStopped) {
                        onHandshakeComplete(connection, capability);
                        responseHandlerMap.remove(connection.getUid());
                        connectionListeners.forEach(listener -> listener.onInboundConnection(connection));
                    }
                });
        responseHandlerMap.put(connection.getUid(), capabilityResponseHandler);
    }

    @Override
    public void onOutboundConnection(OutboundConnection connection, Address peerAddress) {
        if (isStopped) {
            return;
        }
        Optional<Address> optionalAddress = getMyAddress();
        checkArgument(optionalAddress.isPresent(), "My address need to be already available.");
        Address myAddress = optionalAddress.get();
        CapabilityRequestHandler capabilityRequestHandler = new CapabilityRequestHandler(connection,
                peerAddress, myAddress, mySupportedNetworks);
        requestHandlerMap.put(connection.getUid(), capabilityRequestHandler);
        capabilityRequestHandler.request()
                .whenComplete((capability, throwable) -> {
                    if (capability != null && !isStopped) {
                        onHandshakeComplete(connection, capability);
                        requestHandlerMap.remove(connection.getUid());
                        connectionListeners.forEach(listener -> listener.onOutboundConnection(connection, peerAddress));
                    }
                });
    }

    @Override
    public void onDisconnect(Connection connection, Optional<Address> optionalAddress) {
        String uid = connection.getUid();

        MapUtils.disposeAndRemove(uid, requestHandlerMap);
        MapUtils.disposeAndRemove(uid, requestHandlerMap);

        if (sendQueue.containsKey(uid)) {
            sendQueue.get(uid).forEach(tuple ->
                    tuple.second.completeExceptionally(new Exception("Connection has been closed while the message was pending to be sent.")));
            sendQueue.remove(uid);
        }

        capabilityMap.remove(connection.getUid());

        Optional<Address> address = optionalAddress.isPresent() ? optionalAddress : getPeerAddress(connection);
        connectionListeners.forEach(listener -> listener.onDisconnect(connection, address));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Connection> send(Message message, Address peerAddress) {
        return getConnection(peerAddress)
                .thenCompose(connection -> send(message, connection));
    }


    public CompletableFuture<Connection> send(Message message, Connection connection) {
        if (capabilityMap.containsKey(connection.getUid())) {
            return node.send(message, connection);
        } else {
            return addToSendQueue(message, connection);
        }
    }

    public void shutdown() {
        synchronized (isStoppedLock) {
            isStopped = true;
        }
        connectionListeners.clear();
        messageListeners.clear();

        node.removeMessageListener(this);
        node.removeConnectionListener(this);

        sendQueue.values().stream()
                .flatMap(Collection::stream)
                .forEach(tuple -> tuple.second.cancel(true));
        sendQueue.clear();

        MapUtils.disposeAndRemoveAll(requestHandlerMap);
        MapUtils.disposeAndRemoveAll(responseHandlerMap);
        capabilityMap.clear();
    }

    public Optional<Address> getPeerAddress(Connection connection) {
        if (node.getPeerAddress(connection).isPresent()) {
            return node.getPeerAddress(connection);
        } else if (capabilityMap.containsKey(connection.getUid())) {
            return Optional.of(capabilityMap.get(connection.getUid()).getAddress());
        } else {
            return Optional.empty();
        }
    }

    public Capability getCapability(Connection connection) {
        return capabilityMap.get(connection.getUid());
    }

    public Optional<Connection> findConnection(Address peerAddress) {
        Optional<OutboundConnection> fromOutbound = node.getOutboundConnections().stream()
                .filter(c -> peerAddress.equals(c.getAddress()))
                .findAny();
        if (fromOutbound.isPresent()) {
            return Optional.of(fromOutbound.get());
        }

        Optional<InboundConnection> fromInbound = node.getInboundConnections().stream()
                .filter(c -> capabilityMap.get(c.getUid()).getAddress().equals(peerAddress))
                .findAny();
        if (fromInbound.isPresent()) {
            return Optional.of(fromInbound.get());
        }

        return Optional.empty();
    }

    public Set<OutboundConnection> getConnectionsWithSupportedNetwork(NetworkType networkType) {
        Map<String, OutboundConnection> map = node.getOutboundConnections().stream()
                .collect(Collectors.toMap(Connection::getUid, Function.identity()));
        return capabilityMap.entrySet().stream()
                .filter(e -> e.getValue().getSupportedNetworkTypes().contains(networkType))
                .filter(e -> map.containsKey(e.getKey()))
                .map(e -> map.get(e.getKey()))
                .collect(Collectors.toSet());
    }

    public void addConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.add(connectionListener);
    }

    public void removeConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.remove(connectionListener);
    }

    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        messageListeners.remove(messageListener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Delegates
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<GetServerSocketResult> initializeServer(String serverId, int serverPort) {
        return node.initializeServer(serverId, serverPort);
    }

    public Optional<Address> getMyAddress() {
        return node.getMyAddress();
    }

    // In case we have an inbound connection with that address we use that
    public CompletableFuture<Connection> getConnection(Address peerAddress) {
        return capabilityMap.entrySet().stream()
                .filter(entry -> entry.getValue().getAddress().equals(peerAddress))
                .flatMap(entry -> node.findConnection(entry.getKey()).stream())
                .findAny()
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> node.getConnection(peerAddress));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void onHandshakeComplete(Connection connection, Capability capability) {
        processSendQueue(connection);

        String uid = connection.getUid();
        capabilityMap.put(uid, capability);
        log.info("onHandshakeComplete: peers address: {}, connection: {}", capability.getAddress(), connection);

        // Only after we have completed the initial handshake we register for messages to notify our
        // clients for new messages.
        connection.addMessageListener(this);
    }

    private CompletableFuture<Connection> addToSendQueue(Message message, Connection connection) {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        String uid = connection.getUid();
        sendQueue.putIfAbsent(uid, new ArrayList<>());
        List<Tuple2<Message, CompletableFuture<Connection>>> list = sendQueue.get(uid);
        list.add(new Tuple2<>(message, future));
        return future;
    }

    private void processSendQueue(Connection connection) {
        if (isStopped) {
            return;
        }
        String uid = connection.getUid();
        if (sendQueue.containsKey(uid)) {
            List<Tuple2<Message, CompletableFuture<Connection>>> list = sendQueue.get(uid);
            list.forEach(tuple -> {
                Message message = tuple.first;
                CompletableFuture<Connection> future = tuple.second;
                node.send(message, connection)
                        .exceptionally(e -> null)
                        .whenComplete((connection2, throwable2) -> {
                            if (connection2 != null) {
                                future.complete(connection2);
                            } else {
                                future.completeExceptionally(throwable2);
                            }
                        });
            });
            sendQueue.remove(uid);
        }
    }
}
