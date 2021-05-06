package misq.p2p.proxy;

import lombok.extern.slf4j.Slf4j;
import misq.p2p.NetworkConfig;
import misq.p2p.node.Address;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@Slf4j
public class ClearNetNetworkProxy implements NetworkProxy {
    private volatile State state = State.NOT_STARTED;

    public ClearNetNetworkProxy(NetworkConfig networkConfig) {
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("Initialize");
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                Thread.sleep(200); // simulate tor delay
            } catch (InterruptedException ignore) {
            }
            state = State.INITIALIZED;
            future.complete(true);
        }).start();
        return future;
    }

    @Override
    public CompletableFuture<ServerInfo> createServerSocket(String serverId, int serverPort) {
        CompletableFuture<ServerInfo> future = new CompletableFuture<>();
        log.info("Create serverSocket");
        try {
            Thread.sleep(200); // simulate tor delay
        } catch (InterruptedException ignore) {
        }

        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            Address address = new Address(serverSocket.getInetAddress().getHostName(), serverSocket.getLocalPort());
            state = State.SERVER_SOCKET_CREATED;
            log.info("ServerSocket created");
            future.complete(new ServerInfo(serverId, serverSocket, address));
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public Socket getSocket(Address address) {
        log.info("Create new Socket");
        try {
            return new Socket(address.getHost(), address.getPort());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void shutdown() {
        state = State.SHUT_DOWN;
    }

    @Override
    public Optional<Address> getServerAddress(String serverId) {
        return Optional.empty();
    }

    @Override
    public State getState() {
        return state;
    }
}
