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
import misq.common.util.CollectionUtil;
import misq.common.util.MapUtils;
import misq.p2p.Address;
import misq.p2p.node.Connection;
import misq.p2p.node.ConnectionListener;
import misq.p2p.node.Node;
import misq.p2p.peers.Peer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Responsible for executing the peer exchange protocol with the given peer.
 * We use the PeerExchangeStrategy for the selection of nodes and strategy for bootstrap.
 * We could use a set of different strategies and select randomly one. This could increase network resilience.
 */
@Slf4j
public class PeerExchangeManager implements ConnectionListener {
    public static final int TIMEOUT = 300;

    private final Node node;
    private final PeerExchangeStrategy peerExchangeStrategy;
    private final Map<String, PeerExchangeResponseHandler> responseHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, PeerExchangeRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private final Object isStoppedLock = new Object();
    private final PeerExchangeGraph peerExchangeGraph;
    private volatile boolean isStopped;

    public PeerExchangeManager(Node node, PeerExchangeStrategy peerExchangeStrategy) {
        this.node = node;
        this.peerExchangeStrategy = peerExchangeStrategy;

        peerExchangeGraph = new PeerExchangeGraph();
        node.addConnectionListener(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ConnectionListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onConnection(Connection connection) {
        Address peerAddress = connection.getPeerAddress();
        Set<Peer> peersForPeerExchange = peerExchangeStrategy.getPeersForPeerExchange(peerAddress);
        PeerExchangeResponseHandler responseHandler = new PeerExchangeResponseHandler(node,
                connection.getId(),
                peersForPeerExchange,
                peers -> {
                    log.error("PeerExchangeManager.onConnection {}", connection);
                    if (!isStopped) {
                        peerExchangeStrategy.addPeersFromPeerExchange(peers, peerAddress);
                        // We do not remove the handler as we might do repeated exchanges
                    }
                });
        responseHandlerMap.put(connection.getId(), responseHandler);
    }


    @Override
    public void onDisconnect(Connection connection) {
        String connectionId = connection.getId();
        MapUtils.disposeAndRemove(connectionId, requestHandlerMap);
        MapUtils.disposeAndRemove(connectionId, requestHandlerMap);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> bootstrap() {
        Address myAddress = node.getMyAddress();
        Set<Address> addressesForBootstrap = peerExchangeStrategy.getAddressesForBootstrap();
        List<CompletableFuture<Boolean>> allFutures = addressesForBootstrap.stream()
                .map(address -> exchangeWithPeer(address)
                        .whenComplete((success, throwable) -> {
                            // log.error("exchangeWithPeer whenComplete myAddress={} peerAddress={}", myAddress, address);
                            peerExchangeGraph.add(myAddress, address);
                        })
                )
                .collect(Collectors.toList());
        if (allFutures.isEmpty()) {
            //  maybeRepeatBootstrap(myAddress, 0, 0);
            return CompletableFuture.completedFuture(true);
        } else {
            return CollectionUtil.allOf(allFutures)
                    .whenComplete((s, e) -> {
                        log.error("");
                    })                            // We require all futures the be completed
                    .thenApply(resultList -> {
                        return resultList.stream().filter(e -> e).count();
                    })
                    .orTimeout(TIMEOUT, TimeUnit.SECONDS)
                    .thenCompose(numSuccess -> {
                        //  maybeRepeatBootstrap(myAddress, numSuccess, allFutures.size());
                        // Even we don't have any connection (first peer in network case) we return true.
                        return CompletableFuture.completedFuture(true);
                    }).whenComplete((s, e) -> {
                        log.error("");
                    });
        }
    }

    public void shutdown() {
        synchronized (isStoppedLock) {
            isStopped = true;
        }
        node.removeConnectionListener(this);
        MapUtils.disposeAndRemoveAll(requestHandlerMap);
        MapUtils.disposeAndRemoveAll(responseHandlerMap);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<Boolean> exchangeWithPeer(Address peerAddress) {
        return node.getConnection(peerAddress)
                .thenCompose(connection -> {
                    PeerExchangeRequestHandler requestHandler = new PeerExchangeRequestHandler(node, connection.getId());
                    requestHandlerMap.put(connection.getId(), requestHandler);
                    return requestHandler.request(peerExchangeStrategy.getPeersForPeerExchange(peerAddress), peerAddress);
                })
                .thenCompose(peers -> {
                    peerExchangeStrategy.addPeersFromPeerExchange(peers, peerAddress);
                    return CompletableFuture.completedFuture(true);
                })
                .exceptionally(e -> {
                    log.error(e.toString());
                    return false;
                });
    }

    private void maybeRepeatBootstrap(Address myAddress, long numSuccess, int numFutures) {
        boolean repeatBootstrap = peerExchangeStrategy.repeatBootstrap(numSuccess, numFutures);
        if (repeatBootstrap && !isStopped) {
            log.warn("{} repeats the bootstrap call. numSuccess={}; numFutures={}",
                    myAddress,
                    numSuccess,
                    numFutures);
            new Timer("PeerExchange.repeatBootstrap-" + myAddress).schedule(new TimerTask() {
                public void run() {
                    if (!isStopped) {
                        bootstrap();
                    }
                }
            }, peerExchangeStrategy.getRepeatBootstrapDelay());
        }
    }
}
