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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Represents an inbound or outbound connection to a peer node.
 * Listens for messages from the peer.
 * Sends messages to the peer.
 * Notifies messageListeners on new received messages.
 * Notifies errorHandler on exceptions from the inputHandlerService executor.
 */
@Slf4j
public abstract class Connection {
    private ExecutorService outputExecutor;
    private ExecutorService inputHandler;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();
    private final Socket socket;
    @Getter
    protected final String uid = UUID.randomUUID().toString();
    private final Object isStoppedLock = new Object();
    private volatile boolean isStopped;

    public Connection(Socket socket) {
        this.socket = socket;
    }

    public void listen(Consumer<Exception> errorHandler) throws IOException {
        outputExecutor = ThreadingUtils.getSingleThreadExecutor("Connection.outputExecutor-" + getShortId());
        inputHandler = ThreadingUtils.getSingleThreadExecutor("Connection.inputHandler-" + getShortId());

        // TODO java serialisation is just for dev, will be replaced by custom serialisation
        // ObjectOutputStream need to be set before objectInputStream otherwise we get blocked...
        // https://stackoverflow.com/questions/14110986/new-objectinputstream-blocks/14111047
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectInputStream = new ObjectInputStream(socket.getInputStream());

        inputHandler.execute(() -> {
            while (!isStopped && !Thread.currentThread().isInterrupted()) {
                Object object;
                try {
                    object = objectInputStream.readObject();
                    //todo move check to node?
                    if (object instanceof Message) {
                        Message message = (Message) object;
                        log.debug("Received message: {} at connection: {}", message, this);
                        messageListeners.forEach(listener -> listener.onMessage(this, message));
                    } else {
                        throw new Exception("Received object is not of type Message: " + object.getClass().getName());
                    }
                } catch (Exception exception) {
                    if (!isStopped) {
                        close();
                    }
                    errorHandler.accept(exception);
                }
            }
        });
    }

    public CompletableFuture<Connection> send(Message message) {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        outputExecutor.execute(() -> {
            try {
                objectOutputStream.writeObject(message);
                objectOutputStream.flush();
                log.debug("Message sent: {} at connection: {}", message, this);
                future.complete(this);
            } catch (IOException exception) {
                if (!isStopped) {
                    close();
                }
                future.completeExceptionally(exception);
            }
        });
        return future;
    }


    public void close() {
        if (isStopped) {
            return;
        }

        synchronized (isStoppedLock) {
            isStopped = true;
        }
        ThreadingUtils.shutdownAndAwaitTermination(inputHandler);
        ThreadingUtils.shutdownAndAwaitTermination(outputExecutor);
        try {
            socket.close();
        } catch (IOException ignore) {
        }
    }

    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        messageListeners.remove(messageListener);
    }

    protected String getId() {
        return uid;
    }

    protected String getShortId() {
        return uid.substring(0, 8);
    }


    @Override
    public String toString() {
        return getId();
    }
}
