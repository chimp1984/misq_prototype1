package misq.p2p.proxy;

import lombok.extern.slf4j.Slf4j;
import misq.p2p.NetworkConfig;
import misq.p2p.endpoint.Address;

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
        log.debug("Initialize");
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                Thread.sleep(5); // simulate tor delay
            } catch (InterruptedException ignore) {
            }
            state = State.INITIALIZED;
            future.complete(true);
        }).start();
        return future;
    }

    @Override
    public CompletableFuture<GetServerSocketResult> getServerSocket(String serverId, int serverPort) {
        CompletableFuture<GetServerSocketResult> future = new CompletableFuture<>();
        log.debug("Create serverSocket");
        try {
            Thread.sleep(5); // simulate tor delay
        } catch (InterruptedException ignore) {
        }

        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            Address address = Address.localHost(serverPort);
            state = State.SERVER_SOCKET_CREATED;
            log.debug("ServerSocket created");
            future.complete(new GetServerSocketResult(serverId, serverSocket, address));
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        log.debug("Create new Socket");
        return new Socket(address.getHost(), address.getPort());
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
