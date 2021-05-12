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
import misq.p2p.guard.Guard;
import misq.p2p.node.*;
import misq.p2p.peers.Peer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Responsible for executing the peer exchange protocol with the given peer.
 * We use the PeerExchangeStrategy for the selection of nodes and strategy for bootstrap.
 * We could use a set of different strategies and select randomly one. This could increase network resilience.
 */
@Slf4j
public class PeerExchange implements ConnectionListener {
    public static final int TIMEOUT = 300;

    private final Guard guard;
    private final PeerExchangeStrategy strategy;
    private final Map<String, PeerExchangeResponseHandler> responseHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, PeerExchangeRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private final Object isStoppedLock = new Object();
    private final PeerExchangeGraph peerExchangeGraph;
    private volatile boolean isStopped;

    public PeerExchange(Guard guard, PeerExchangeStrategy strategy) {
        this.guard = guard;
        this.strategy = strategy;

        peerExchangeGraph = new PeerExchangeGraph();
        guard.addConnectionListener(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ConnectionListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onInboundConnection(InboundConnection connection) {
        // Capability exchange with address has been completed already so expect to know the peers address.
        // We do not add the requesters address to the reported peers.
        guard.getPeerAddress(connection).ifPresent(address -> {
            Set<Peer> peersForPeerExchange = strategy.getPeersForPeerExchange(address);
            PeerExchangeResponseHandler responseHandler = new PeerExchangeResponseHandler(connection,
                    peersForPeerExchange,
                    peers -> {
                        if (!isStopped) {
                            strategy.addPeersFromPeerExchange(peers);
                            responseHandlerMap.remove(connection.getUid());
                        }
                    });
            responseHandlerMap.put(connection.getUid(), responseHandler);
        });
    }

    @Override
    public void onOutboundConnection(OutboundConnection connection, Address peerAddress) {
    }

    @Override
    public void onDisconnect(Connection connection, Optional<Address> optionalAddress) {
        String uid = connection.getUid();
        MapUtils.disposeAndRemove(uid, requestHandlerMap);
        MapUtils.disposeAndRemove(uid, requestHandlerMap);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> bootstrap() {
        Optional<Address> optionalMyAddress = guard.getMyAddress();
        checkArgument(optionalMyAddress.isPresent(),
                "We must have already done out capability exchange so the peers address need to be known");
        Address myAddress = optionalMyAddress.get();
        Set<Address> addressesForBootstrap = strategy.getAddressesForBootstrap();
        log.info("bootstrap {} to {}", optionalMyAddress, addressesForBootstrap);
        List<CompletableFuture<Boolean>> allFutures = addressesForBootstrap.stream()
                .map(address -> exchangeWithPeer(address)
                        .whenComplete((success, throwable) -> peerExchangeGraph.add(myAddress, address))
                )
                .collect(Collectors.toList());
        if (allFutures.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        } else {
            return CollectionUtil.allOf(allFutures)                                 // We require all futures the be completed
                    .thenApply(resultList -> resultList.stream().filter(e -> e).count())
                    .orTimeout(TIMEOUT, TimeUnit.SECONDS)
                    .thenCompose(numSuccess -> {
                        maybeRepeatBootstrap(myAddress, numSuccess, allFutures.size());
                        // Even we don't have any connection (first peer in network case) we return true.
                        return CompletableFuture.completedFuture(true);
                    });
        }
    }

    public void shutdown() {
        synchronized (isStoppedLock) {
            isStopped = true;
        }
        guard.removeConnectionListener(this);
        MapUtils.disposeAndRemoveAll(requestHandlerMap);
        MapUtils.disposeAndRemoveAll(responseHandlerMap);
    }

    private CompletableFuture<Boolean> exchangeWithPeer(Address peerAddress) {
        return guard.getConnection(peerAddress)
                .thenCompose(connection -> {
                    PeerExchangeRequestHandler requestHandler = new PeerExchangeRequestHandler(connection);
                    requestHandlerMap.put(connection.getUid(), requestHandler);
                    return requestHandler.request(strategy.getPeersForPeerExchange(peerAddress));
                })
                .thenCompose(peers -> {
                    strategy.addPeersFromPeerExchange(peers);
                    return CompletableFuture.completedFuture(true);
                })
                .exceptionally(e -> false);
    }

    private void maybeRepeatBootstrap(Address myAddress, long numSuccess, int numFutures) {
        boolean repeatBootstrap = strategy.repeatBootstrap(numSuccess, numFutures);
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
            }, strategy.getRepeatBootstrapDelay());
        }
    }
}
