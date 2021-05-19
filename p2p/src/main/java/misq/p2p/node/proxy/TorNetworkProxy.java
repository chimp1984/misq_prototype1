package misq.p2p.node.proxy;

import lombok.extern.slf4j.Slf4j;
import misq.common.util.FileUtils;
import misq.p2p.Address;
import misq.p2p.NetworkConfig;
import misq.p2p.NetworkId;
import misq.torify.Constants;
import misq.torify.Tor;
import misq.torify.TorServerSocket;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.io.File.separator;


@Slf4j
public class TorNetworkProxy implements NetworkProxy {
    public final static int DEFAULT_PORT = 9999;

    private final String torDirPath;
    private final NetworkId networkId;
    private final Tor tor;

    public TorNetworkProxy(NetworkConfig networkConfig) {
        torDirPath = networkConfig.getNetworkId().getBaseDirPath() + separator + "tor";
        networkId = networkConfig.getNetworkId();

        // We get a singleton instance per application (torDirPath)
        tor = Tor.getTor(torDirPath);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("Initialize Tor");
        long ts = System.currentTimeMillis();
        return tor.startAsync()
                .thenApply(result -> {
                    log.info("Tor initialized after {} ms", System.currentTimeMillis() - ts);
                    return true;
                });
    }

    @Override
    public CompletableFuture<GetServerSocketResult> getServerSocket(String serverId, int serverPort) {
        log.info("Start hidden service");
        long ts = System.currentTimeMillis();
        try {
            TorServerSocket torServerSocket = tor.getTorServerSocket();
            return torServerSocket.bindAsync(networkId.getServerPort(), networkId.getId())
                    .thenApply(onionAddress -> {
                        log.info("Tor hidden service Ready. Took {} ms. Onion address={}", System.currentTimeMillis() - ts, onionAddress);
                        return new GetServerSocketResult(networkId.getId(), torServerSocket, new Address(onionAddress.getHost(), onionAddress.getPort()));
                    });
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        long ts = System.currentTimeMillis();
        Socket socket = tor.getSocket(null);
        socket.connect(new InetSocketAddress(address.getHost(), address.getPort()));
        log.info("Tor socket to {} created. Took {} ms", address, System.currentTimeMillis() - ts);
        return socket;
    }

    // todo
    //  public Socks5Proxy getSocksProxy() {

    @Override
    public void shutdown() {
        if (tor != null) {
            tor.shutdown();
        }
    }

    //todo move to torify lib
    @Override
    public Optional<Address> getServerAddress(String serverId) {
        String fileName = torDirPath + separator + Constants.HS_DIR + separator + serverId + separator + "hostname";
        if (new File(fileName).exists()) {
            try {
                String host = FileUtils.readAsString(fileName);
                return Optional.of(new Address(host, TorNetworkProxy.DEFAULT_PORT));
            } catch (IOException e) {
                log.error(e.toString(), e);
            }
        }

        return Optional.empty();
    }
}
