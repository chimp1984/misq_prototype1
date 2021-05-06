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

package misq.p2p.router;

import misq.p2p.guard.Guard;
import misq.p2p.node.Address;
import misq.p2p.node.Connection;
import misq.p2p.node.Message;
import misq.p2p.node.MessageListener;
import misq.p2p.router.gossip.GossipResult;
import misq.p2p.router.gossip.GossipRouter;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Responsibility:
 * - Supports multiple routers
 * - Decides which router is used for which message
 * - MessageListeners will get the consolidated messages from multiple routers
 */
public class Router implements MessageListener {
    private final GossipRouter gossipRouter;
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();

    public Router(Guard guard) {
        gossipRouter = new GossipRouter(guard);
        gossipRouter.addMessageListener(this);
    }

    public CompletableFuture<GossipResult> broadcast(Message message) {
        return gossipRouter.broadcast(message);
    }

    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        messageListeners.remove(messageListener);
    }

    @Override
    public void onMessage(Connection connection, Message message) {
        messageListeners.forEach(listener -> listener.onMessage(connection, message));
    }

    public Address getPeerAddressesForInventoryRequest() {
        return gossipRouter.getPeerAddressesForInventoryRequest();
    }

    public void shutdown() {
        messageListeners.clear();
        gossipRouter.removeMessageListener(this);
        gossipRouter.shutdown();
    }
}
