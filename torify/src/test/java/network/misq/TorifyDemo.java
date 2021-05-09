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

package network.misq;

import com.runjva.sourceforge.jsocks.protocol.SocksSocket;
import misq.common.util.OsUtils;
import misq.torify.OnionAddress;
import misq.torify.TorController;
import misq.torify.TorServerSocket;
import misq.torify.Torify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class TorifyDemo {
    private static final Logger log = LoggerFactory.getLogger(TorifyDemo.class);

    public static void main(String[] args) {
        String torDirPath = OsUtils.getUserDataDir() + "/TorifyDemo";
        //  useBlockingAPI(torDirPath);
        useNonBlockingAPI(torDirPath);
    }

    private static void useBlockingAPI(String torDirPath) {
        try {
            Torify torify = new Torify(torDirPath);
            TorController torController = torify.start();
            TorServerSocket torServerSocket = startServer(torDirPath, torController);
            OnionAddress onionAddress = torServerSocket.getOnionAddress().get();
            sendViaSocketFactory(torify, onionAddress);
            sendViaProxy(torify, onionAddress);
            sendViaSocket(torify, onionAddress);
            sendViaSocksSocket(torify, onionAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void useNonBlockingAPI(String torDirPath) {
        AtomicBoolean stopped = new AtomicBoolean(false);

        Torify torify = new Torify(torDirPath);
        torify.startAsync()
                .exceptionally(throwable -> {
                    log.error(throwable.toString());
                    throwable.printStackTrace();
                    return null;
                })
                .thenAccept(torController -> {
                    if (torController == null) {
                        return;
                    }

                    startServerAsync(torDirPath, torController)
                            .exceptionally(throwable -> {
                                log.error(throwable.toString());
                                throwable.printStackTrace();
                                return null;
                            })
                            .thenAccept(onionAddress -> {
                                if (onionAddress == null) {
                                    return;
                                }

                                sendViaSocketFactory(torify, onionAddress);
                                sendViaProxy(torify, onionAddress);
                                sendViaSocket(torify, onionAddress);
                                sendViaSocksSocket(torify, onionAddress);
                                stopped.set(true);
                            });
                });

        while (!stopped.get()) {
        }
    }

    private static TorServerSocket startServer(String torDirPath,
                                               TorController torController) throws IOException, InterruptedException {
        try {
            TorServerSocket torServerSocket = new TorServerSocket(torDirPath, torController);
            File hsDir = new File(torDirPath, "hiddenservice_2");
            torServerSocket.bind(4000, 9999, hsDir);
            runServer(torServerSocket);
            return torServerSocket;
        } catch (IOException | InterruptedException e) {
            throw e;
        }
    }

    private static CompletableFuture<OnionAddress> startServerAsync(String torDirPath,
                                                                    TorController torController) {
        CompletableFuture<OnionAddress> future = new CompletableFuture<>();
        try {
            TorServerSocket torServerSocket = new TorServerSocket(torDirPath, torController);
            File hsDir = new File(torDirPath, "hiddenservice_3");
            torServerSocket
                    .bindAsync(3000, 4444, hsDir)
                    .whenComplete((onionAddress, throwable) -> {
                        if (throwable == null) {
                            runServer(torServerSocket);
                            future.complete(onionAddress);
                        } else {
                            future.completeExceptionally(throwable);
                        }
                    });
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    private static void runServer(TorServerSocket torServerSocket) {
        new Thread(() -> {
            Thread.currentThread().setName("Server");
            while (true) {
                try {
                    log.info("Start listening for new connections on {}", torServerSocket.getOnionAddress());
                    Socket clientSocket = torServerSocket.accept();
                    createInboundConnection(clientSocket);
                } catch (IOException e) {
                    try {
                        torServerSocket.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }).start();
    }

    private static void createInboundConnection(Socket clientSocket) {
        log.info("New client connection accepted");
        new Thread(() -> {
            Thread.currentThread().setName("Read at inbound connection");
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                 ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream())) {
                objectOutputStream.flush();
                listenOnInputStream(clientSocket, objectInputStream, "inbound connection");
            } catch (IOException e) {
                try {
                    clientSocket.close();
                } catch (IOException ignore) {
                }
            }
        }).start();
    }

    private static void listenOnInputStream(Socket socket, ObjectInputStream objectInputStream, String info) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Object object = objectInputStream.readObject();
                log.info("Received at {} {}", info, object);
            }
        } catch (IOException | ClassNotFoundException e) {
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
    }

    // Outbound connection
    private static void sendViaSocket(Torify torify, OnionAddress onionAddress) {
        try {
            Socket socket = torify.getSocket("test_stream_id");
            socket.connect(new InetSocketAddress(onionAddress.getHost(), onionAddress.getPort()));
            sendOnOutboundConnection(socket, "test via Socket");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendViaSocksSocket(Torify torify, OnionAddress onionAddress) {
        try {
            SocksSocket socket = torify.getSocksSocket(onionAddress.getHost(), onionAddress.getPort(), "test_stream_id");
            sendOnOutboundConnection(socket, "test via SocksSocket");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendViaSocketFactory(Torify torify, OnionAddress onionAddress) {
        try {
            SocketFactory socketFactory = torify.getSocketFactory("test_stream_id");
            Socket socket = socketFactory.createSocket(onionAddress.getHost(), onionAddress.getPort());
            sendOnOutboundConnection(socket, "test via SocketFactory");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendViaProxy(Torify torify, OnionAddress onionAddress) {
        try {
            Proxy proxy = torify.getProxy("test_stream_id");
            Socket socket = new Socket(proxy);
            socket.connect(new InetSocketAddress(onionAddress.getHost(), onionAddress.getPort()));
            sendOnOutboundConnection(socket, "test via Proxy");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendOnOutboundConnection(Socket socket, String msg) {
        log.info("sendViaOutboundConnection {}", msg);
        new Thread(() -> {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream())) {
                objectOutputStream.writeObject(msg);
                objectOutputStream.flush();
                listenOnInputStream(socket, objectInputStream, "outbound connection");
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
            }
        }).start();
    }
}
