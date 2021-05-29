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

package misq;


import misq.p2p.Address;
import misq.p2p.NetworkId;
import misq.p2p.NetworkType;
import misq.p2p.P2pService;
import misq.p2p.data.filter.DataFilter;
import misq.p2p.data.inventory.RequestInventoryResult;
import misq.p2p.message.Message;
import misq.p2p.node.Connection;
import misq.p2p.node.MessageListener;
import misq.p2p.node.proxy.GetServerSocketResult;
import misq.p2p.router.gossip.GossipResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MockP2pService extends P2pService {
    private static final Logger log = LoggerFactory.getLogger(MockP2pService.class);
    private Set<MessageListener> messageListeners = ConcurrentHashMap.newKeySet();

    public MockP2pService() {
        super();
    }

    @Override
    public CompletableFuture<Boolean> initializeServer(BiConsumer<GetServerSocketResult, Throwable> resultHandler) {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> bootstrap() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Connection> confidentialSend(Message message, NetworkId networkId, KeyPair myKeyPair) {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
            future.complete(null);

            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
            messageListeners.forEach(e -> e.onMessage(message, null));
        }).start();

        return future;
    }

    @Override
    public void requestAddData(Message message,
                               Consumer<GossipResult> resultHandler) {
    }

    @Override
    public void requestRemoveData(Message message,
                                  Consumer<GossipResult> resultHandler) {
    }

    @Override
    public void requestInventory(DataFilter dataFilter,
                                 Consumer<RequestInventoryResult> resultHandler) {
    }

    @Override
    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    @Override
    public void removeMessageListener(MessageListener messageListener) {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public Optional<Address> findMyAddress(NetworkType networkType) {
        return Optional.empty();
    }
}
