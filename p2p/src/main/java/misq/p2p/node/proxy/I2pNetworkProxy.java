package misq.p2p.node.proxy;

import lombok.extern.slf4j.Slf4j;
import misq.common.util.SystemUtils;
import misq.i2p.SamClient;
import misq.p2p.Address;
import misq.p2p.NetworkConfig;
import misq.p2p.node.RawNode;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.io.File.separator;

// Start I2P
// Enable SAM at http://127.0.0.1:7657/configclients
// Takes about 1 minute until its ready
@Slf4j
public class I2pNetworkProxy implements NetworkProxy {
    private final String i2pDirPath;
    private volatile State state = State.NOT_STARTED;
    // We only use one SamClient (for tests we would create multiple instances of nodes, but we still want 1 client)
    private static SamClient samClient;

    public I2pNetworkProxy(NetworkConfig networkConfig) {
        i2pDirPath = networkConfig.getBaseDirName() + separator + "i2p";
    }

    public CompletableFuture<Boolean> initialize() {
        log.debug("Initialize");
        try {
            if (samClient == null) {
                samClient = new SamClient(i2pDirPath);
            }
            state = State.INITIALIZED;
            return CompletableFuture.completedFuture(true);
        } catch (Exception exception) {
            log.error(exception.toString(), exception);
            shutdown();
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    public CompletableFuture<GetServerSocketResult> getServerSocket(String serverId, int serverPort) {
        CompletableFuture<GetServerSocketResult> future = new CompletableFuture<>();
        log.debug("Create serverSocket");
        new Thread(() -> {
            try {
                ServerSocket serverSocket = samClient.getServerSocket(serverId, SystemUtils.findFreeSystemPort());
                String destination = samClient.getMyDestination(serverId);
                Address address = new Address(destination, -1);
                log.debug("Create new Socket to {}", address);
                state = State.SERVER_SOCKET_CREATED;
                log.debug("ServerSocket created for address {}", address);
                future.complete(new GetServerSocketResult(serverId, serverSocket, address));
            } catch (Exception exception) {
                log.error(exception.toString(), exception);
                shutdown();
                future.completeExceptionally(exception);
            }
        }).start();
        return future;
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        try {
            log.debug("Create new Socket to {}", address);
            //todo pass session id
            Socket socket = samClient.connect(address.getHost(), RawNode.DEFAULT_SERVER_ID + "Alice");
            log.debug("Created new Socket");
            return socket;
        } catch (IOException exception) {
            log.error(exception.toString(), exception);
            shutdown();
            throw exception;
        }
    }

    @Override
    public void shutdown() {
        if (samClient != null) {
            samClient.shutDown();
        }
        state = State.SHUT_DOWN;
    }

    @Override
    public Optional<Address> getServerAddress(String serverId) {
        try {
            String myDestination = samClient.getMyDestination(serverId);
            return Optional.of(new Address(myDestination, -1));
        } catch (IOException exception) {
            log.error(exception.toString(), exception);
            return Optional.empty();
        }
    }

    @Override
    public State getState() {
        return state;
    }
}
