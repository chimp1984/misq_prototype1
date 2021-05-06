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
import misq.p2p.proxy.NetworkProxy;
import misq.p2p.proxy.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

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

    protected final NetworkConfig networkConfig;
    private final NetworkProxy networkProxy;

    private final Map<Address, OutboundConnection> outboundConnectionMap = new ConcurrentHashMap<>();
    private final Set<InboundConnection> inboundConnections = new CopyOnWriteArraySet<>();
    private final Set<ConnectionListener> connectionListeners = new CopyOnWriteArraySet<>();
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();
    private final Map<String, Server> serverMap = new ConcurrentHashMap<>();

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
    public CompletableFuture<ServerInfo> bootstrap() {
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
    public CompletableFuture<ServerInfo> createServerAndListen(String serverId, int serverPort) {
        return networkProxy.createServerSocket(serverId, serverPort)
                .whenComplete((serverInfo, throwable) -> {
                    if (serverInfo != null) {
                        Server server = new Server(serverInfo,
                                this::onClientSocket,
                                exception -> serverMap.remove(serverId));
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

    public void disconnect(Address address) {
        log.info("disconnect {}", address);
        if (outboundConnectionMap.containsKey(address)) {
            disconnect(outboundConnectionMap.get(address));
        }
    }

    public void disconnect(Connection connection) {
        log.info("disconnect connection {}", connection);
        connection.close();
        if (connection instanceof InboundConnection) {
            inboundConnections.remove(connection);
        } else if (connection instanceof OutboundConnection) {
            outboundConnectionMap.remove(((OutboundConnection) connection).getAddress());
        }
        connection.removeMessageListener(this);
        connectionListeners.forEach(connectionListener -> connectionListener.onDisconnect(connection));
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
        return connection.send(message);
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

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<Boolean> initialize() {
        return networkProxy.initialize();
    }

    private void onClientSocket(Socket socket) {
        try {
            InboundConnection connection = new InboundConnection(socket, exception -> {
                exception.printStackTrace();
                log.error(exception.toString());
            });
            inboundConnections.add(connection);
            log.info("New inbound connection");
            connectionListeners.forEach(listener -> listener.onInboundConnection(connection));
            connection.addMessageListener(this);
        } catch (IOException exception) {
            exception.printStackTrace();
            log.error(exception.toString());
        }
    }

    private CompletableFuture<Connection> createConnection(Address peerAddress) {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        try {
            Socket socket = getSocket(peerAddress);
            OutboundConnection connection = new OutboundConnection(socket,
                    peerAddress,
                    future::completeExceptionally);
            outboundConnectionMap.put(peerAddress, connection);
            log.info("New outbound connection to {}", peerAddress);
            connectionListeners.forEach(listener -> listener.onOutboundConnection(connection, peerAddress));
            connection.addMessageListener(this);
            future.complete(connection);
        } catch (IOException exception) {
            exception.printStackTrace();
            log.error(exception.toString());
            future.completeExceptionally(exception);
        }
        return future;
    }
}
