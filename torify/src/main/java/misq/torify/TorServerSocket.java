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
import misq.common.util.FileUtils;
import misq.common.util.ThreadingUtils;
import net.freehaven.tor.control.TorControlConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static misq.torify.Constants.*;

public class TorServerSocket extends ServerSocket {
    private static final Logger log = LoggerFactory.getLogger(TorServerSocket.class);

    private final TorEventHandler eventHandler = new TorEventHandler();
    private final String torDirPath;
    private final TorController torController;

    private Optional<OnionAddress> onionAddress = Optional.empty();
    @Nullable
    private ExecutorService executor;

    public TorServerSocket(String torDirPath,
                           TorController torController) throws IOException {
        this.torDirPath = torDirPath;
        this.torController = torController;

        torController.setEventHandler(eventHandler);
    }

    public CompletableFuture<OnionAddress> bindAsync(int hiddenServicePort) {
        return bindAsync(hiddenServicePort, hiddenServicePort);
    }

    public CompletableFuture<OnionAddress> bindAsync(int hiddenServicePort, int localPort) {
        return bindAsync(hiddenServicePort, localPort, new File(torDirPath, HS_DIR));
    }

    public CompletableFuture<OnionAddress> bindAsync(int hiddenServicePort, int localPort, File hsDir) {
        return bindAsync(hiddenServicePort, localPort, hsDir, getAndSetExecutor());
    }

    public CompletableFuture<OnionAddress> bindAsync(int hiddenServicePort,
                                                     int localPort,
                                                     File hsDir,
                                                     @Nullable Executor executor) {
        CompletableFuture<OnionAddress> future = new CompletableFuture<>();
        if (executor == null) {
            executor = MoreExecutors.directExecutor();
        }
        executor.execute(() -> {
            Thread.currentThread().setName("TorServerSocket.bind");
            try {
                bind(hiddenServicePort, localPort, hsDir);
                checkArgument(onionAddress.isPresent(), "onionAddress must be present");
                future.complete(onionAddress.get());
            } catch (IOException | InterruptedException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    // Blocking
    public void bind(int hiddenServicePort, int localPort, File hsDir) throws IOException, InterruptedException {
        log.debug("Start bind TorServerSocket");
        long ts = System.currentTimeMillis();

        File hostNameFile = new File(hsDir.getCanonicalPath(), HOSTNAME);
        File privKeyFile = new File(hsDir.getCanonicalPath(), PRIV_KEY);
        FileUtils.makeDirs(hsDir);

        TorControlConnection.CreateHiddenServiceResult result;
        if (privKeyFile.exists()) {
            String privateKey = FileUtils.readFromFile(privKeyFile);
            result = torController.createHiddenService(hiddenServicePort, localPort, privateKey);
        } else {
            result = torController.createHiddenService(hiddenServicePort, localPort);
        }

        if (!hostNameFile.exists()) {
            FileUtils.makeFile(hostNameFile);
        }
        String serviceId = result.serviceID;

        OnionAddress onionAddress = new OnionAddress(serviceId + ".onion", hiddenServicePort);
        FileUtils.writeToFile(onionAddress.getHost(), hostNameFile);
        this.onionAddress = Optional.of(onionAddress);

        if (!privKeyFile.exists()) {
            FileUtils.makeFile(privKeyFile);
        }
        FileUtils.writeToFile(result.privateKey, privKeyFile);

        log.debug("Start publishing hidden service {}", onionAddress);
        CountDownLatch latch = new CountDownLatch(1);
        eventHandler.putHiddenServiceReadyListener(serviceId, () -> {
            try {
                super.bind(new InetSocketAddress(LOCALHOST, localPort));
                log.info(">> TorServerSocket ready. Took {} ms", System.currentTimeMillis() - ts);
                latch.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        latch.await();
    }

    @Override
    public void close() throws IOException {
        super.close();

        onionAddress.ifPresent(onionAddress -> {
            eventHandler.removeHiddenServiceReadyListener(onionAddress.getServiceId());
            try {
                torController.destroyHiddenService(onionAddress.getServiceId());
            } catch (IOException ignore) {
            }
        });
        if (executor != null) {
            ThreadingUtils.shutdownAndAwaitTermination(executor);
        }
    }

    public Optional<OnionAddress> getOnionAddress() {
        return onionAddress;
    }

    private ExecutorService getAndSetExecutor() {
        executor = ThreadingUtils.getSingleThreadExecutor("TorServerSocket.bindAsync");
        return executor;
    }
}
