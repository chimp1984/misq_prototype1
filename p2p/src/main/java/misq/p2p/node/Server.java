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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.common.util.ThreadingUtils;
import misq.p2p.proxy.ServerInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Slf4j
public class Server {
    private final ServerSocket serverSocket;
    private final ExecutorService executorService;
    @Getter
    private final Address address;
    private final Object isStoppedLock = new Object();
    private volatile boolean isStopped;

    /**
     * Server using the given ServerSocket.
     *
     * @param serverInfo       contains serverSocket and address
     * @param socketHandler    Consumes socket on new inbound connection
     * @param exceptionHandler
     */
    public Server(ServerInfo serverInfo, Consumer<Socket> socketHandler, Consumer<Exception> exceptionHandler) {
        this.serverSocket = serverInfo.getServerSocket();

        address = serverInfo.getAddress();
        executorService = ThreadingUtils.getSingleThreadExecutor("Server-" + serverInfo);
        executorService.execute(() -> {
            while (isNotStopped()) {
                try {
                    Socket socket = serverSocket.accept();
                    log.info("Accepted new connection on server: {}", serverInfo);
                    if (isNotStopped()) {
                        socketHandler.accept(socket);
                    }
                } catch (IOException e) {
                    exceptionHandler.accept(e);
                    stop();
                }
            }
        });
    }

    private boolean isNotStopped() {
        return !isStopped && !Thread.currentThread().isInterrupted();
    }

    public void stop() {
        synchronized (isStoppedLock) {
            isStopped = true;
        }
        ThreadingUtils.shutdownAndAwaitTermination(executorService);
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
