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

package misq.p2p.node.connection;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.common.util.ThreadingUtils;
import misq.p2p.Address;
import misq.p2p.node.proxy.GetServerSocketResult;

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
     * @param getServerSocketResult contains serverSocket and address
     * @param socketHandler         Consumes socket on new inbound connection
     * @param exceptionHandler
     */
    public Server(GetServerSocketResult getServerSocketResult, Consumer<Socket> socketHandler, Consumer<Exception> exceptionHandler) {
        this.serverSocket = getServerSocketResult.getServerSocket();

        address = getServerSocketResult.getAddress();
        log.debug("Create server: {}", getServerSocketResult);
        executorService = ThreadingUtils.getSingleThreadExecutor("Server-" + getServerSocketResult);
        executorService.execute(() -> {
            while (isNotStopped()) {
                try {
                    Socket socket = serverSocket.accept();
                    log.debug("Accepted new connection on server: {}", getServerSocketResult);
                    if (isNotStopped()) {
                        socketHandler.accept(socket);
                    }
                } catch (IOException e) {
                    if (!isStopped) {
                        exceptionHandler.accept(e);
                        stop();
                    }
                }
            }
        });
    }

    private boolean isNotStopped() {
        return !isStopped && !Thread.currentThread().isInterrupted();
    }

    public void stop() {
        if (isStopped) {
            return;
        }
        synchronized (isStoppedLock) {
            isStopped = true;
        }
        ThreadingUtils.shutdownAndAwaitTermination(executorService);
        try {
            serverSocket.close();
        } catch (IOException ignore) {
        }
    }
}
