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

package misq.p2p.peers;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import misq.p2p.node.Node;
import misq.p2p.peers.exchange.PeerExchangeManager;
import misq.p2p.peers.exchange.PeerExchangeStrategy;

import java.util.concurrent.CompletableFuture;

/**
 * Coordinating different specialized managers for bootstrapping into the network and maintaining a healthy
 * group of neighbor peers.
 * <p>
 * It starts by bootstrapping the network of neighbor peers using the PeerExchange which provides information of
 * other available peers in the network.
 * After the initial network of mostly outbound connections has been created we switch strategy to maintain a preferred
 * composition of peers managed in PeerGroupHealth.
 * <p>
 * <p>
 * Strategy (using default numbers from config):
 * 1. Bootstrap
 * Use 2 seed nodes and 8 persisted nodes (using PeerExchangeSelection) for the exchange protocol
 * - Send our reported and connected peers in the exchange protocol (using PeerExchangeSelection) and add the
 * ones reported back from the peer to our reported peers.
 * - Once a connection is established the peer gets added to connected peers inside peer group
 * 2. Repeat until sufficient connections are reached
 * Once the CompletableFuture from PeerExchange.bootstrap completes we check how many connection attempts have led to successful
 * connections.
 * - If we have reached our target of 8 connections we stop and return true to the calling client.
 * - If we did not reach our target we repeat after a delay the exchange protocol using reported peers
 * (using PeerExchangeSelection).
 */
@Slf4j
public class PeerManager {

    private final Node node;
    private final PeerGroup peerGroup;
    private final PeerConfig peerConfig;
    private final PeerExchangeManager peerExchangeManager;
    private final PeerGroupHealth peerGroupHealth;
    private final Object isStoppedLock = new Object();
    private volatile boolean isStopped;

    public PeerManager(Node node,
                       PeerGroup peerGroup,
                       PeerExchangeStrategy peerExchangeStrategy,
                       PeerConfig peerConfig) {
        this.node = node;
        this.peerGroup = peerGroup;
        this.peerConfig = peerConfig;

        peerExchangeManager = new PeerExchangeManager(node, peerExchangeStrategy);
        peerGroupHealth = new PeerGroupHealth(node, peerGroup);
    }

    public CompletableFuture<Boolean> bootstrap(String serverId, int serverPort) {
        return node.initializeServer(serverId, serverPort)
                .thenCompose(serverInfo -> peerExchangeManager.bootstrap())
                .thenCompose(completed -> peerGroupHealth.bootstrap());
    }

    public void shutdown() {
        if (isStopped) {
            return;
        }
        synchronized (isStoppedLock) {
            isStopped = true;
        }
        peerExchangeManager.shutdown();
        peerGroupHealth.shutdown();
    }

    public boolean sufficientPeersAtPeerExchange() {
        return sufficientConnections() && sufficientReportedPeers();
    }

    private boolean sufficientReportedPeers() {
        return peerGroup.getReportedPeers().size() >= peerConfig.getMinNumReportedPeers();
    }

    private boolean sufficientConnections() {
        return peerGroup.getAllConnectedPeers().size() >= peerConfig.getMinNumConnectedPeers();
    }

    @VisibleForTesting
    public PeerGroup getPeerGroup() {
        return peerGroup;
    }

    @VisibleForTesting
    public Node getGuard() {
        return node;
    }
}
