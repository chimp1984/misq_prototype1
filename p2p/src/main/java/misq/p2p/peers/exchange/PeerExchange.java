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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * At bootstrap we exchange peers with a provided set of addresses.
 */
@Slf4j
public class PeerExchange implements ConnectionListener {
    public static final int TIMEOUT = 300;

    private final Guard guard;
    private final PeerExchangeSelection peerExchangeSelection;
    private final Map<String, PeerExchangeResponseHandler> responseHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, PeerExchangeRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();

    public PeerExchange(Guard guard, PeerExchangeSelection peerExchangeSelection) {
        this.guard = guard;
        this.peerExchangeSelection = peerExchangeSelection;

        guard.addConnectionListener(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ConnectionListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onInboundConnection(InboundConnection connection) {
        PeerExchangeResponseHandler responseHandler = new PeerExchangeResponseHandler(connection,
                peerExchangeSelection.getPeersForPeerExchange(),
                peers -> {
                    peerExchangeSelection.addPeersFromPeerExchange(peers);
                    responseHandlerMap.remove(connection.getUid());
                });
        responseHandlerMap.put(connection.getUid(), responseHandler);
    }

    @Override
    public void onOutboundConnection(OutboundConnection connection, Address peerAddress) {
    }

    @Override
    public void onDisconnect(Connection connection) {
        String uid = connection.getUid();
        MapUtils.disposeAndRemove(uid, requestHandlerMap);
        MapUtils.disposeAndRemove(uid, requestHandlerMap);
    }

    public CompletableFuture<Boolean> bootstrap() {
        List<CompletableFuture<Boolean>> futureList = peerExchangeSelection.getAddressesForBoostrap().stream()
                .map(this::exchangePeers)
                .collect(Collectors.toList());
        return CollectionUtil.allOf(futureList)
                .thenApply(resultList -> resultList.stream().anyMatch(e -> e))
                .thenCompose(anySuccess -> {
                    if (peerExchangeSelection.sufficientPeersAtPeerExchange()) {
                        return CompletableFuture.completedFuture(true);
                    } else {
                        log.warn("We did not succeed with any or our peer exchange attempts. We try again.");
                        return bootstrap();
                    }
                })
                .orTimeout(TIMEOUT, TimeUnit.SECONDS);
    }

    private CompletableFuture<Boolean> exchangePeers(Address peerAddress) {
        return guard.getConnection(peerAddress)
                .thenCompose(connection -> {
                    PeerExchangeRequestHandler requestHandler = new PeerExchangeRequestHandler(connection);
                    requestHandlerMap.put(connection.getUid(), requestHandler);
                    return requestHandler.request(peerExchangeSelection.getPeersForPeerExchange());
                })
                .thenCompose(peers -> {
                    peerExchangeSelection.addPeersFromPeerExchange(peers);
                    return CompletableFuture.completedFuture(true);
                })
                .exceptionally(e -> false);
    }

    public void shutdown() {
        guard.removeConnectionListener(this);
        MapUtils.disposeAndRemoveAll(requestHandlerMap);
        MapUtils.disposeAndRemoveAll(responseHandlerMap);
    }
}
