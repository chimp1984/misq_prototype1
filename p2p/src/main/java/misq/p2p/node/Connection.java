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
    private final Socket socket;
    private final ExecutorService outputExecutorService;
    private final ExecutorService inputHandlerService;

    @Getter
    protected final String uid = UUID.randomUUID().toString();

    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();
    private final ObjectInputStream objectInputStream;
    private final ObjectOutputStream objectOutputStream;
    private volatile boolean isStopped;
    private final Object isStoppedLock = new Object();

    public Connection(Socket socket, Consumer<Exception> errorHandler) throws IOException {
        this.socket = socket;

        outputExecutorService = ThreadingUtils.getSingleThreadExecutor("Connection.outputExecutorService-" + getId());
        inputHandlerService = ThreadingUtils.getSingleThreadExecutor("Connection.inputHandlerService-" + getId());

        // ObjectOutputStream need to be set before objectInputStream otherwise we get blocked...
        // https://stackoverflow.com/questions/14110986/new-objectinputstream-blocks/14111047
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectInputStream = new ObjectInputStream(socket.getInputStream());

        inputHandlerService.execute(() -> {
            while (!isStopped && !Thread.currentThread().isInterrupted()) {
                Object object;
                try {
                    object = objectInputStream.readObject();
                    log.info("Received {}", object);
                    //todo keep check outside?
                    if (object instanceof Message) {
                        Message message = (Message) object;
                        messageListeners.forEach(listener -> listener.onMessage(this, message));
                    } else {
                        throw new Exception("Received object is not of type Message: " + object.getClass().getName());
                    }
                } catch (Exception exception) {
                    errorHandler.accept(exception);
                    close();
                }
            }
        });
    }

    public CompletableFuture<Connection> send(Message message) {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        outputExecutorService.execute(() -> {
            try {
                objectOutputStream.writeObject(message);
                objectOutputStream.flush();
                log.info("Message sent: {}", message);
                future.complete(this);
            } catch (IOException exception) {
                close();
                future.completeExceptionally(exception);
            }
        });
        return future;
    }


    public void close() {
        synchronized (isStoppedLock) {
            isStopped = true;
        }
        ThreadingUtils.shutdownAndAwaitTermination(inputHandlerService);
        ThreadingUtils.shutdownAndAwaitTermination(outputExecutorService);
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

}
