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

package misq.p2p.router.gossip;

import misq.common.util.CollectionUtil;
import misq.p2p.guard.Guard;
import misq.p2p.node.Address;
import misq.p2p.node.Connection;
import misq.p2p.node.Message;
import misq.p2p.node.MessageListener;
import misq.p2p.peers.PeerGroup;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Responsibility:
 * - Creates PeerGroup for peer management
 * - Broadcasts messages to peers provided by PeerGroup
 * - Notifies MessageListeners on messages which have been sent by via a GossipMessage
 */
public class GossipRouter implements MessageListener {
    private static final long BROADCAST_TIMEOUT = 90;

    private final Guard guard;
    private final PeerGroup peerGroup;
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();

    public GossipRouter(Guard guard) {
        this.guard = guard;
        peerGroup = new PeerGroup(guard);

        guard.addMessageListener(this);
    }

    @Override
    public void onMessage(Connection connection, Message message) {
        if (message instanceof GossipMessage) {
            GossipMessage gossipMessage = (GossipMessage) message;
            messageListeners.forEach(listener -> listener.onMessage(connection, gossipMessage.getMessage()));
        }
    }

    public CompletableFuture<GossipResult> broadcast(Message message) {
        long ts = System.currentTimeMillis();
        CompletableFuture<GossipResult> future = new CompletableFuture<>();
        future.orTimeout(BROADCAST_TIMEOUT, TimeUnit.SECONDS);
        AtomicInteger numSuccess = new AtomicInteger(0);
        AtomicInteger numFaults = new AtomicInteger(0);
        Set<Address> connectedPeerAddresses = peerGroup.getConnectedPeerAddresses();
        int target = connectedPeerAddresses.size();
        connectedPeerAddresses.forEach(address -> {
            guard.send(new GossipMessage(message), address)
                    .whenComplete((connection, t) -> {
                        if (connection != null) {
                            numSuccess.incrementAndGet();
                        } else {
                            numFaults.incrementAndGet();
                        }
                        if (numSuccess.get() + numFaults.get() == target) {
                            future.complete(new GossipResult(numSuccess.get(),
                                    numFaults.get(),
                                    System.currentTimeMillis() - ts));
                        }
                    });
        });
        return future;
    }

    public Address getPeerAddressesForInventoryRequest() {
        return CollectionUtil.getRandomElement(peerGroup.getConnectedPeerAddresses());
    }

    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        messageListeners.remove(messageListener);
    }

    public void shutdown() {
        messageListeners.clear();

        guard.removeMessageListener(this);
        peerGroup.shutdown();
    }
}
