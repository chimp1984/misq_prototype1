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

package misq.p2p.peers.exchange;

import lombok.extern.slf4j.Slf4j;
import misq.common.util.Disposable;
import misq.p2p.Address;
import misq.p2p.message.Message;
import misq.p2p.node.Connection;
import misq.p2p.node.MessageListener;
import misq.p2p.node.Node;
import misq.p2p.peers.Peer;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PeerExchangeRequestHandler implements MessageListener, Disposable {
    private static final long TIMEOUT_SEC = 90;

    private final Node node;
    private final String connectionId;
    private final CompletableFuture<Set<Peer>> future = new CompletableFuture<>();

    public PeerExchangeRequestHandler(Node node, String connectionId) {
        this.node = node;
        this.connectionId = connectionId;
    }

    public CompletableFuture<Set<Peer>> request(Set<Peer> peers, Address peerAddress) {
        future.orTimeout(TIMEOUT_SEC, TimeUnit.SECONDS);
        node.addMessageListener(this);
        node.send(new PeerExchangeRequest(peers), peerAddress);
        return future;
    }

    public void dispose() {
        node.removeMessageListener(this);
        future.cancel(true);
    }

    @Override
    public void onMessage(Message message, Connection connection) {
        if (connectionId.equals(connection.getId()) && message instanceof PeerExchangeResponse) {
            PeerExchangeResponse peerExchangeResponse = (PeerExchangeResponse) message;
            node.removeMessageListener(this);
            future.complete(peerExchangeResponse.getPeers());
        }
    }
}
