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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.common.util.ThreadingUtils;
import misq.p2p.message.Message;

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

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents an inbound or outbound connection to a peer node.
 * Listens for messages from the peer.
 * Sends messages to the peer.
 * Notifies messageListeners on new received messages.
 * Notifies errorHandler on exceptions from the inputHandlerService executor.
 */
@Slf4j
public abstract class RawConnection {
    public interface MessageListener {
        void onMessage(Message message);
    }

    @Getter
    @EqualsAndHashCode
    public static class MisqMessage implements Message {
        private final Message payload;

        public MisqMessage(Message payload) {
            this.payload = payload;
        }

        @Override
        public String toString() {
            return "MisqMessage{" +
                    ",\n     payload=" + payload +
                    "\n}";
        }
    }

    private ExecutorService outputExecutor;
    private ExecutorService inputHandler;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();
    private final Socket socket;
    protected final String id = UUID.randomUUID().toString();
    private final Object isStoppedLock = new Object();
    private volatile boolean isStopped;

    protected RawConnection(Socket socket) {
        this.socket = socket;
    }

    public void listen(Consumer<Exception> errorHandler) throws IOException {
        outputExecutor = ThreadingUtils.getSingleThreadExecutor("Connection.outputExecutor-" + getShortId());
        inputHandler = ThreadingUtils.getSingleThreadExecutor("Connection.inputHandler-" + getShortId());

        // TODO java serialisation is just for dev, will be replaced by custom serialisation
        // Type-Length-Value Format is considered to be used:
        // https://github.com/lightningnetwork/lightning-rfc/blob/master/01-messaging.md#type-length-value-format
        // ObjectOutputStream need to be set before objectInputStream otherwise we get blocked...
        // https://stackoverflow.com/questions/14110986/new-objectinputstream-blocks/14111047
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectInputStream = new ObjectInputStream(socket.getInputStream());

        inputHandler.execute(() -> {
            while (!isStopped && !Thread.currentThread().isInterrupted()) {
                Object object;
                try {
                    object = objectInputStream.readObject();
                    checkArgument(object instanceof MisqMessage,
                            "Received object is not of type MisqMessage: " + object.getClass().getName());
                    MisqMessage misqMessage = (MisqMessage) object;
                    log.debug("Received message: {} at connection: {}", misqMessage, this);
                    messageListeners.forEach(listener -> listener.onMessage(misqMessage.getPayload()));
                } catch (Exception exception) {
                    //todo StreamCorruptedException from i2p at shutdown. prob it send some text data at shut down
                    close();
                    errorHandler.accept(exception);
                }
            }
        });
    }

    public CompletableFuture<RawConnection> send(Message message) {
        CompletableFuture<RawConnection> future = new CompletableFuture<>();
        outputExecutor.execute(() -> {
            try {
                MisqMessage misqMessage = new MisqMessage(message);
                objectOutputStream.writeObject(misqMessage);
                objectOutputStream.flush();
                log.debug("Message sent: {} at connection: {}", misqMessage, this);
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

    public String getId() {
        return id;
    }

    private String getShortId() {
        return id.substring(0, 24);
    }

    @Override
    public String toString() {
        return id;
    }
}
