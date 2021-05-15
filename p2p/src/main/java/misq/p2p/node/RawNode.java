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
import misq.p2p.Message;
import misq.p2p.NetworkConfig;
import misq.p2p.node.connection.InboundConnection;
import misq.p2p.node.connection.OutboundConnection;
import misq.p2p.node.connection.RawConnection;
import misq.p2p.node.connection.Server;
import misq.p2p.node.proxy.GetServerSocketResult;
import misq.p2p.node.proxy.NetworkProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

/**
 * Responsibility:
 * - Creates network proxy based on networkConfig via NetworkProxy factory method.
 * - Creates Servers kept in a map by serverId.
 * - Creates inbound and outbound connections.
 * - Checks if a connection has been created when sending a message and creates one otherwise.
 * - Notifies ConnectionListeners when a new connection has been created or one has been closed.
 */
public class RawNode {
    private static final Logger log = LoggerFactory.getLogger(RawNode.class);
    public static final String DEFAULT_SERVER_ID = "default";
    public static final int DEFAULT_SERVER_PORT = 9999;

    interface ConnectionListener {
        void onInboundConnection(InboundConnection inboundConnection);

        void onOutboundConnection(OutboundConnection outboundConnection, Address peerAddress);

        void onDisconnect(RawConnection rawConnection);
    }

    private final NetworkProxy networkProxy;
    private final Map<String, Server> serverMap = new ConcurrentHashMap<>();
    private final Map<Address, OutboundConnection> outboundConnectionMap = new ConcurrentHashMap<>();
    private final Set<InboundConnection> inboundConnections = new CopyOnWriteArraySet<>();
    private final Set<ConnectionListener> connectionListeners = new CopyOnWriteArraySet<>();
    private final Object isStoppedLock = new Object();
    private volatile boolean isStopped;

    RawNode(NetworkConfig networkConfig) {
        networkProxy = NetworkProxy.get(networkConfig);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Combines initialize and createServerAndListen
     *
     * @return ServerInfo
     */
    CompletableFuture<GetServerSocketResult> initializeServer(String serverId, int serverPort) {
        return networkProxy.initialize()
                .thenCompose(e -> createServerAndListen(serverId, serverPort));
    }

    /**
     * Creates server socket and starts server
     *
     * @param serverId
     * @param serverPort
     * @return
     */
    CompletableFuture<GetServerSocketResult> createServerAndListen(String serverId, int serverPort) {
        return networkProxy.getServerSocket(serverId, serverPort)
                .whenComplete((serverInfo, throwable) -> {
                    if (serverInfo != null) {
                        Server server = new Server(serverInfo,
                                socket -> onClientSocket(socket, serverInfo),
                                exception -> {
                                    serverMap.remove(serverId);
                                    handleException(exception);
                                });
                        serverMap.put(serverId, server);
                    }
                });
    }

    CompletableFuture<RawConnection> getOrCreateConnection(Address peerAddress) {
        if (outboundConnectionMap.containsKey(peerAddress)) {
            RawConnection rawConnection = outboundConnectionMap.get(peerAddress);
            return CompletableFuture.completedFuture(rawConnection);
        } else {
            return createConnection(peerAddress);
        }
    }

    Optional<RawConnection> findConnection(String connectionUid) {
        return Stream.concat(outboundConnectionMap.values().stream(), inboundConnections.stream())
                .filter(e -> e.getId().equals(connectionUid))
                .findAny();
    }

    void disconnect(RawConnection connection) {
        log.info("disconnect connection {}", connection);
        connection.close();
        onDisconnect(connection);
    }


    /**
     * Sends to outbound connection if available, otherwise create the connection and then send the message.
     * At that layer we do not send to an potentially existing inbound connection as we do not know the peers address
     * at inbound connections. Higher layers can utilize that and use the send(Message message, RawConnection connection)
     * method instead.
     *
     * @param message
     * @param address
     * @return
     */
    CompletableFuture<RawConnection> send(Message message, Address address) {
        return getOrCreateConnection(address)
                .thenCompose(connection -> send(message, connection));
    }

    CompletableFuture<RawConnection> send(Message message, RawConnection connection) {
        return connection.send(message)
                .exceptionally(exception -> {
                    handleException(connection, exception);
                    return connection;
                });
    }

    void shutdown() {
        if (isStopped) {
            return;
        }
        synchronized (isStoppedLock) {
            isStopped = true;
        }

        connectionListeners.clear();

        serverMap.values().forEach(Server::stop);
        serverMap.clear();
        outboundConnectionMap.values().forEach(RawConnection::close);
        outboundConnectionMap.clear();
        inboundConnections.forEach(RawConnection::close);
        inboundConnections.clear();

        networkProxy.shutdown();
    }

    Optional<Address> findMyAddress() {
        return findMyAddress(DEFAULT_SERVER_ID);
    }

    Optional<Address> findPeerAddress(RawConnection connection) {
        if (connection instanceof OutboundConnection) {
            return Optional.of(((OutboundConnection) connection).getAddress());
        } else {
            return Optional.empty();
        }
    }

    void addConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.add(connectionListener);
    }

    void removeConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.remove(connectionListener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private Socket getSocket(Address address) throws IOException {
        return networkProxy.getSocket(address);
    }

    private Optional<Address> findMyAddress(String serverId) {
        if (serverMap.containsKey(serverId)) {
            return Optional.of(serverMap.get(serverId).getAddress());
        } else {
            return Optional.empty();
        }
    }

    private void onClientSocket(Socket socket, GetServerSocketResult getServerSocketResult) {
        try {
            InboundConnection connection = new InboundConnection(socket, getServerSocketResult);
            connection.listen(exception -> handleException(connection, exception));
            inboundConnections.add(connection);
            connectionListeners.forEach(listener -> listener.onInboundConnection(connection));
        } catch (IOException exception) {
            handleException(exception);
        }
    }

    private CompletableFuture<RawConnection> createConnection(Address peerAddress) {
        CompletableFuture<RawConnection> future = new CompletableFuture<>();
        RawConnection rawConnection = null;
        try {
            Socket socket = getSocket(peerAddress);
            log.debug("Create new outbound connection to {}", peerAddress);
            OutboundConnection outboundConnection = new OutboundConnection(socket, peerAddress);
            rawConnection = outboundConnection;
            outboundConnection.listen(exception -> {
                handleException(outboundConnection, exception);
                future.completeExceptionally(exception);
            });

            outboundConnectionMap.put(peerAddress, outboundConnection);
            connectionListeners.forEach(listener -> listener.onOutboundConnection(outboundConnection, peerAddress));
            future.complete(outboundConnection);
        } catch (IOException exception) {
            if (rawConnection == null) {
                handleException(exception);
            } else {
                handleException(rawConnection, exception);
            }
            future.completeExceptionally(exception);
        }
        return future;
    }

    private void onDisconnect(RawConnection connection) {
        if (connection instanceof InboundConnection) {
            inboundConnections.remove(connection);
        } else if (connection instanceof OutboundConnection) {
            OutboundConnection outboundConnection = (OutboundConnection) connection;
            Address peerAddress = outboundConnection.getAddress();
            outboundConnectionMap.remove(peerAddress);
        }
        connectionListeners.forEach(connectionListener -> connectionListener.onDisconnect(connection));
    }

    private void handleException(Throwable exception) {
        if (isStopped) {
            return;
        }
        if (exception instanceof EOFException) {
            log.debug(exception.toString(), exception);
        } else if (exception instanceof SocketException) {
            log.debug(exception.toString(), exception);
        } else {
            log.error(exception.toString(), exception);
        }
    }

    private void handleException(RawConnection connection, Throwable exception) {
        if (isStopped) {
            return;
        }
        handleException(exception);
        onDisconnect(connection);
    }
}
