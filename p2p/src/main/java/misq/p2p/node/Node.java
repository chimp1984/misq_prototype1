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


import misq.p2p.NetworkConfig;
import misq.p2p.proxy.GetServerSocketResult;
import misq.p2p.proxy.NetworkProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

/**
 * Responsibility:
 * - Creates networkProxy from given networkType
 * - Creates multiple Servers kept in a map by serverId.
 * - Creates inbound and outbound connections.
 * - Checks if a connection has been created when sending a message and creates one otherwise.
 * - Notifies ConnectionListeners when a new connection has been created or one has been closed.
 */
public class Node implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(Node.class);
    public static final String DEFAULT_SERVER_ID = "default";
    public static final int DEFAULT_SERVER_PORT = 9999;

    private final NetworkConfig networkConfig;
    private final NetworkProxy networkProxy;

    private final Map<String, Server> serverMap = new ConcurrentHashMap<>();
    private final Map<Address, OutboundConnection> outboundConnectionMap = new ConcurrentHashMap<>();
    private final Set<InboundConnection> inboundConnections = new CopyOnWriteArraySet<>();
    private final Set<ConnectionListener> connectionListeners = new CopyOnWriteArraySet<>();
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();
    private final Object isStoppedLock = new Object();
    private volatile boolean isStopped;

    public Node(NetworkConfig networkConfig) {
        this.networkConfig = networkConfig;

        networkProxy = NetworkProxy.get(networkConfig);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Connection connection, Message message) {
        messageListeners.forEach(listener -> listener.onMessage(connection, message));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Combines initialize and createServerAndListen
     *
     * @return ServerInfo
     */
    public CompletableFuture<GetServerSocketResult> initializeServer() {
        return initialize()
                .thenCompose(e -> createServerAndListen(networkConfig.getServerId(), networkConfig.getServerPort()));
    }

    /**
     * Creates server socket and starts server
     *
     * @param serverId
     * @param serverPort
     * @return
     */
    public CompletableFuture<GetServerSocketResult> createServerAndListen(String serverId, int serverPort) {
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

    public Socket getSocket(Address address) throws IOException {
        return networkProxy.getSocket(address);
    }

    public CompletableFuture<Connection> getConnection(Address peerAddress) {
        if (outboundConnectionMap.containsKey(peerAddress)) {
            Connection connection = outboundConnectionMap.get(peerAddress);
            return CompletableFuture.completedFuture(connection);
        } else {
            return createConnection(peerAddress);
        }
    }

    public Optional<Connection> findConnection(String connectionUid) {
        return Stream.concat(outboundConnectionMap.values().stream(), inboundConnections.stream())
                .filter(e -> e.getUid().equals(connectionUid))
                .findAny();
    }

    public void disconnect(Connection connection) {
        log.info("disconnect connection {}", connection);
        connection.close();
        onDisconnect(connection);
    }

    public void onDisconnect(Connection connection) {
        if (connection instanceof InboundConnection) {
            inboundConnections.remove(connection);
        } else if (connection instanceof OutboundConnection) {
            outboundConnectionMap.remove(((OutboundConnection) connection).getAddress());
        }
        connection.removeMessageListener(this);
        connectionListeners.forEach(connectionListener -> connectionListener.onDisconnect(connection, getPeerAddress(connection)));
    }


    /**
     * Sends to outbound connection if available, otherwise create the connection and then send the message.
     *
     * @param message
     * @param address
     * @return
     */
    public CompletableFuture<Connection> send(Message message, Address address) {
        if (!outboundConnectionMap.containsKey(address)) {
            return getConnection(address)
                    .thenCompose(connection -> send(message, connection));
        } else {
            return send(message, outboundConnectionMap.get(address));
        }
    }

    public CompletableFuture<Connection> send(Message message, Connection connection) {
        return connection.send(message)
                .exceptionally(exception -> {
                    handleException(connection, exception);
                    return connection;
                });
    }

    public Optional<Address> getMyAddress() {
        return getMyAddress(DEFAULT_SERVER_ID);
    }

    public Optional<Address> getMyAddress(String serverId) {
        if (serverMap.containsKey(serverId)) {
            return Optional.of(serverMap.get(serverId).getAddress());
        } else {
            return Optional.empty();
        }
    }

    public Optional<Address> getPeerAddress(Connection connection) {
        if (connection instanceof OutboundConnection) {
            return Optional.of(((OutboundConnection) connection).getAddress());
        } else {
            return Optional.empty();
        }
    }

    public void shutdown() {
        log.info("shutdown {}", getMyAddress());
        if (isStopped) {
            return;
        }
        synchronized (isStoppedLock) {
            isStopped = true;
        }

        connectionListeners.clear();
        messageListeners.clear();

        serverMap.values().forEach(Server::stop);
        serverMap.clear();
        outboundConnectionMap.values().forEach(Connection::close);
        outboundConnectionMap.clear();
        inboundConnections.forEach(Connection::close);
        inboundConnections.clear();

        networkProxy.shutdown();
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

    public Collection<OutboundConnection> getOutboundConnections() {
        return outboundConnectionMap.values();
    }

    public Set<InboundConnection> getInboundConnections() {
        return inboundConnections;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<Boolean> initialize() {
        return networkProxy.initialize();
    }

    private void onClientSocket(Socket socket, GetServerSocketResult getServerSocketResult) {
        try {
            InboundConnection connection = new InboundConnection(socket, getServerSocketResult);
            connection.listen(exception -> handleException(connection, exception));
            inboundConnections.add(connection);
            connectionListeners.forEach(listener -> listener.onInboundConnection(connection));
            connection.addMessageListener(this);
        } catch (IOException exception) {
            handleException(exception);
        }
    }

    private CompletableFuture<Connection> createConnection(Address peerAddress) {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        Connection connection = null;
        try {
            Socket socket = getSocket(peerAddress);
            log.debug("Create new outbound connection to {}", peerAddress);
            OutboundConnection outboundConnection = new OutboundConnection(socket, peerAddress);
            connection = outboundConnection;
            outboundConnection.listen(exception -> {
                handleException(outboundConnection, exception);
                future.completeExceptionally(exception);
            });

            outboundConnectionMap.put(peerAddress, outboundConnection);
            connectionListeners.forEach(listener -> listener.onOutboundConnection(outboundConnection, peerAddress));
            outboundConnection.addMessageListener(this);
            future.complete(outboundConnection);
        } catch (IOException exception) {
            if (connection == null) {
                handleException(exception);
            } else {
                handleException(connection, exception);
            }
            future.completeExceptionally(exception);
        }
        return future;
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

    private void handleException(Connection connection, Throwable exception) {
        if (isStopped) {
            return;
        }
        handleException(exception);
        onDisconnect(connection);
    }
}
