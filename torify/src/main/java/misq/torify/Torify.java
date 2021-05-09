/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package misq.torify;

import com.google.common.util.concurrent.MoreExecutors;
import com.runjva.sourceforge.jsocks.protocol.Authentication;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import com.runjva.sourceforge.jsocks.protocol.SocksSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.net.SocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static misq.torify.Constants.LOCALHOST;

public class Torify {
    private static final Logger log = LoggerFactory.getLogger(Torify.class);
    public static final String TOR_SERVICE_VERSION = "0.1.0";

    private final TorController torController;
    private final Bootstrap bootstrap;

    private volatile boolean shutdownRequested;
    @Nullable
    private ExecutorService startupExecutor;
    private int proxyPort = -1;

    public Torify(String torDirPath) {
        bootstrap = new Bootstrap(torDirPath);
        torController = new TorController(bootstrap.getCookieFile());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread.currentThread().setName("Torify.shutdownHook");
            shutdown();
        }));
    }

    public void shutdown() {
        shutdownRequested = true;
        if (startupExecutor != null) {
            MoreExecutors.shutdownAndAwaitTermination(startupExecutor, 100, TimeUnit.MILLISECONDS);
            startupExecutor = null;
        }
        torController.shutdown();
        log.info("Shutdown Tor completed");
    }

    public CompletableFuture<TorController> startAsync() {
        return startAsync(getStartupExecutor());
    }

    public CompletableFuture<TorController> startAsync(Executor executor) {
        CompletableFuture<TorController> future = new CompletableFuture<>();
        checkArgument(!shutdownRequested, "shutdown already requested");
        executor.execute(() -> {
            try {
                TorController torController = start();
                future.complete(torController);
            } catch (IOException | InterruptedException e) {
                bootstrap.deleteVersionFile();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    // Blocking start
    public TorController start() throws IOException, InterruptedException {
        checkArgument(!shutdownRequested, "shutdown already requested");
        long ts = System.currentTimeMillis();
        int controlPort = bootstrap.start();
        torController.start(controlPort);
        proxyPort = torController.getProxyPort();
        log.info(">> Starting Tor took {} ms", System.currentTimeMillis() - ts);
        return torController;
    }

    public SocksSocket getSocksSocket(String remoteHost, int remotePort, @Nullable String streamId) throws IOException {
        checkArgument(!shutdownRequested, "shutdown already requested");
        Socks5Proxy socks5Proxy = getSocks5Proxy(streamId);
        SocksSocket socksSocket = new SocksSocket(socks5Proxy, remoteHost, remotePort);
        socksSocket.setTcpNoDelay(true);
        return socksSocket;
    }

    public Socket getSocket() throws IOException {
        return new Socket(getProxy(null));
    }

    public Socket getSocket(@Nullable String streamId) throws IOException {
        checkArgument(!shutdownRequested, "shutdown already requested");
        return new Socket(getProxy(streamId));
    }

    public Proxy getProxy(@Nullable String streamId) throws IOException {
        checkArgument(!shutdownRequested, "shutdown already requested");
        Socks5Proxy socks5Proxy = getSocks5Proxy(streamId);
        InetSocketAddress socketAddress = new InetSocketAddress(socks5Proxy.getInetAddress(), socks5Proxy.getPort());
        return new Proxy(Proxy.Type.SOCKS, socketAddress);
    }

    public SocketFactory getSocketFactory(@Nullable String streamId) throws IOException {
        checkArgument(!shutdownRequested, "shutdown already requested");
        return new TorSocketFactory(getProxy(streamId));
    }

    public Socks5Proxy getSocks5Proxy(@Nullable String streamId) throws IOException {
        checkArgument(!shutdownRequested, "shutdown already requested");
        checkArgument(proxyPort > -1, "proxyPort must be defined");
        Socks5Proxy socks5Proxy = new Socks5Proxy(LOCALHOST, proxyPort);
        socks5Proxy.resolveAddrLocally(false);
        if (streamId == null) {
            return socks5Proxy;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(streamId.getBytes());
            String asBase26 = new BigInteger(digest).toString(26);
            byte[] hash = asBase26.getBytes();
            // Authentication method ID 2 is User/Password
            socks5Proxy.setAuthenticationMethod(2,
                    new Authentication() {
                        @Override
                        public Object[] doSocksAuthentication(int i, Socket socket) throws IOException {
                            // Must not close streams here, as otherwise we get a socket closed
                            // exception at SocksSocket
                            OutputStream outputStream = socket.getOutputStream();
                            outputStream.write(new byte[]{(byte) 1, (byte) hash.length});
                            outputStream.write(hash);
                            outputStream.write(new byte[]{(byte) 1, (byte) 0});
                            outputStream.flush();

                            byte[] status = new byte[2];
                            InputStream inputStream = socket.getInputStream();
                            if (inputStream.read(status) == -1) {
                                throw new IOException("Did not get data");
                            }
                            if (status[1] != (byte) 0) {
                                throw new IOException("Authentication error: " + status[1]);
                            }
                            return new Object[]{inputStream, outputStream};
                        }
                    });
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return socks5Proxy;
    }

    private ExecutorService getStartupExecutor() {
        startupExecutor = Utils.getSingleThreadExecutor("Torify.startAsync");
        return startupExecutor;
    }
}
