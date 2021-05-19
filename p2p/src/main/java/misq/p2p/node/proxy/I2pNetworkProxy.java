package misq.p2p.node.proxy;

import lombok.extern.slf4j.Slf4j;
import misq.common.util.SystemUtils;
import misq.common.util.ThreadingUtils;
import misq.i2p.SamClient;
import misq.p2p.Address;
import misq.p2p.NetworkConfig;
import misq.p2p.node.RawNode;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.io.File.separator;

// Start I2P
// Enable SAM at http://127.0.0.1:7657/configclients
// Takes about 1-2 minutes until its ready
@Slf4j
public class I2pNetworkProxy implements NetworkProxy {
    private final String i2pDirPath;
    private SamClient samClient;
    private final ExecutorService getServerSocketExecutor = ThreadingUtils.getSingleThreadExecutor("I2pNetworkProxy.ServerSocket");

    public I2pNetworkProxy(NetworkConfig networkConfig) {
        i2pDirPath = networkConfig.getBaseDirPath() + separator + "i2p";
    }

    public CompletableFuture<Boolean> initialize() {
        log.debug("Initialize");
        try {
            samClient = SamClient.getSamClient(i2pDirPath);
            return CompletableFuture.completedFuture(true);
        } catch (Exception exception) {
            log.error(exception.toString(), exception);
            return CompletableFuture.failedFuture(exception);
        }
    }

    @Override
    public CompletableFuture<GetServerSocketResult> getServerSocket(String serverId, int serverPort) {
        CompletableFuture<GetServerSocketResult> future = new CompletableFuture<>();
        log.debug("Create serverSocket");
        getServerSocketExecutor.execute(() -> {
            try {
                ServerSocket serverSocket = samClient.getServerSocket(serverId, SystemUtils.findFreeSystemPort());
                String destination = samClient.getMyDestination(serverId);
                Address address = new Address(destination, -1);
                log.debug("Create new Socket to {}", address);
                log.debug("ServerSocket created for address {}", address);
                future.complete(new GetServerSocketResult(serverId, serverSocket, address));
            } catch (Exception exception) {
                log.error(exception.toString(), exception);
                future.completeExceptionally(exception);
            }
        });
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
            throw exception;
        }
    }

    @Override
    public void shutdown() {
        if (samClient != null) {
            samClient.shutDown();
        }
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
}
