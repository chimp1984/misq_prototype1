package misq.p2p.proxy;

import lombok.extern.slf4j.Slf4j;
import misq.common.util.FileUtil;
import misq.common.util.NetworkUtils;
import misq.p2p.NetworkConfig;
import misq.p2p.node.Address;
import misq.torify.TorController;
import misq.torify.TorServerSocket;
import misq.torify.Torify;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@Slf4j
public class TorNetworkProxy implements NetworkProxy {
    public final static int DEFAULT_PORT = 9999;
    private final String torDirPath;


    private volatile NetworkProxy.State state = State.NOT_STARTED;
    private Torify torify;
    private TorController torController;

    public TorNetworkProxy(NetworkConfig networkConfig) {
        torDirPath = networkConfig.getBaseDirName() + File.separator + "tor";
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("Initialize Tor");
        long ts = System.currentTimeMillis();
        torify = new Torify(torDirPath);
        return torify.startAsync()
                .thenApply(torController -> {
                    state = State.INITIALIZED;
                    this.torController = torController;
                    log.info("Tor initialized after {} ms", System.currentTimeMillis() - ts);
                    return true;
                });
    }

    @Override
    public CompletableFuture<ServerInfo> createServerSocket(String serverId, int serverPort) {
        log.info("Start hidden service");
        long ts = System.currentTimeMillis();
        try {
            TorServerSocket torServerSocket = new TorServerSocket(torDirPath, torController);
            return torServerSocket.bindAsync(serverPort, NetworkUtils.findFreeSystemPort())
                    .thenApply(onionAddress -> {
                        log.info("Tor hidden service Ready. Took {} ms. Onion address={}", System.currentTimeMillis() - ts, onionAddress);
                        state = State.SERVER_SOCKET_CREATED;
                        return new ServerInfo(serverId, torServerSocket, new Address(onionAddress.getHost(), onionAddress.getPort()));
                    });
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        long ts = System.currentTimeMillis();
        Socket socket = torify.getSocket(null);
        socket.connect(new InetSocketAddress(address.getHost(), address.getPort()));
        log.info("Tor socket to {} created. Took {} ms", address, System.currentTimeMillis() - ts);
        return socket;
    }

    // todo
    //  public Socks5Proxy getSocksProxy() {

    @Override
    public void shutdown() {
        state = State.SHUTTING_DOWN;
        if (torify != null) {
            torify.shutdown();
        }
        state = State.SHUT_DOWN;
    }

    @Override
    public Optional<Address> getServerAddress(String serverId) {
        String fileName = torDirPath + File.separator + "hiddenservice" + File.separator + serverId + File.separator + "hostname";
        if (new File(fileName).exists()) {
            try {
                String host = FileUtil.readAsString(fileName);
                return Optional.of(new Address(host, TorNetworkProxy.DEFAULT_PORT));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Optional.empty();
    }

    @Override
    public State getState() {
        return state;
    }

}
