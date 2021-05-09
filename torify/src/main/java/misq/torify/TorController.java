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

import misq.common.util.FileUtils;
import net.freehaven.tor.control.TorControlConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class TorController {
    private static final Logger log = LoggerFactory.getLogger(TorController.class);

    private final File cookieFile;
    @Nullable
    private TorControlConnection torControlConnection;
    @Nullable
    private Socket controlSocket;
    private boolean isStarted;

    TorController(File cookieFile) {
        this.cookieFile = cookieFile;
    }

    void start(int controlPort) throws IOException {
        controlSocket = new Socket("127.0.0.1", controlPort);
        torControlConnection = new TorControlConnection(controlSocket);
        torControlConnection.authenticate(FileUtils.asBytes(cookieFile));
        torControlConnection.setEvents(Constants.EVENTS);
        torControlConnection.takeOwnership();
        torControlConnection.resetConf(Constants.OWNER);
        torControlConnection.setConf(Constants.DISABLE_NETWORK, "0");

        while (true) {
            String status = torControlConnection.getInfo(Constants.STATUS_BOOTSTRAP_PHASE);
            log.debug("Listen on bootstrap progress: >> {}", status);
            if (status != null && status.contains("PROGRESS=100")) {
                log.info("Listen on bootstrap progress: >> {}", status);
                break;
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignore) {
                }
            }
        }
        isStarted = true;
    }

    void shutdown() {
        try {
            if (torControlConnection != null) {
                torControlConnection.setConf(Constants.DISABLE_NETWORK, "1");
                torControlConnection.shutdownTor("TERM");
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error(e.toString());
        } finally {
            try {
                if (controlSocket != null) {
                    try {
                        controlSocket.close();
                    } catch (IOException ignore) {
                    }
                }
            } finally {
                controlSocket = null;
                torControlConnection = null;
            }
        }
    }

    int getProxyPort() throws IOException {
        checkArgument(isStarted, "Startup not completed");
        String socksInfo = checkNotNull(torControlConnection).getInfo(Constants.NET_LISTENERS_SOCKS);
        socksInfo = socksInfo.replace("\"", "");
        String[] tokens = socksInfo.split(":");
        String port = tokens[tokens.length - 1];
        return Integer.parseInt(port);
    }

    void setEventHandler(TorEventHandler eventHandler) {
        checkArgument(isStarted, "Startup not completed");
        checkNotNull(torControlConnection).setEventHandler(eventHandler);
    }

    TorControlConnection.CreateHiddenServiceResult createHiddenService(int hiddenServicePort,
                                                                       int localPort) throws IOException {
        checkArgument(isStarted, "Startup not completed");
        return checkNotNull(torControlConnection).createHiddenService(hiddenServicePort, localPort);
    }

    TorControlConnection.CreateHiddenServiceResult createHiddenService(int hiddenServicePort,
                                                                       int localPort,
                                                                       String privateKey) throws IOException {
        checkArgument(isStarted, "Startup not completed");
        return checkNotNull(torControlConnection).createHiddenService(hiddenServicePort, localPort, privateKey);
    }

    void destroyHiddenService(String serviceId) throws IOException {
        checkArgument(isStarted, "Startup not completed");
        checkNotNull(torControlConnection).destroyHiddenService(serviceId);
    }
}
