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
import misq.common.Disposable;
import misq.p2p.message.Message;
import misq.p2p.node.Connection;
import misq.p2p.node.MessageListener;
import misq.p2p.node.Node;
import misq.p2p.peers.Peer;

import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class PeerExchangeResponseHandler implements MessageListener, Disposable {
    private final Node node;
    private final String connectionId;
    private final Set<Peer> peers;
    private final Consumer<Set<Peer>> resultHandler;

    public PeerExchangeResponseHandler(Node node,
                                       String connectionId,
                                       Set<Peer> peers,
                                       Consumer<Set<Peer>> resultHandler) {
        this.node = node;
        this.connectionId = connectionId;
        this.peers = peers;
        this.resultHandler = resultHandler;
        node.addMessageListener(this);
    }

    public void dispose() {
        node.removeMessageListener(this);
    }

    @Override
    public void onMessage(Message message, Connection connection) {
        if (connectionId.equals(connection.getId()) && message instanceof PeerExchangeRequest) {
            PeerExchangeRequest peerExchangeRequest = (PeerExchangeRequest) message;
            PeerExchangeResponse response = new PeerExchangeResponse(peers);
            node.send(response, connection);
            // We do not remove the MessageListener as we might do repeated exchanges
            resultHandler.accept(peerExchangeRequest.getPeers());
        }
    }

}
